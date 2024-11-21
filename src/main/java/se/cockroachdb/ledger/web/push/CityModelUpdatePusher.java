package se.cockroachdb.ledger.web.push;

import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import se.cockroachdb.ledger.aspect.AdvisorOrder;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.event.TransferCreatedEvent;
import se.cockroachdb.ledger.model.CityModel;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.util.Money;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Order(CityModelUpdatePusher.PRECEDENCE)
public class CityModelUpdatePusher {
    public static final int PRECEDENCE = AdvisorOrder.CHANGE_FEED_ADVISOR;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SimpMessagePublisher simpMessagePublisher;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ReportingRepository reportingRepository;

    private final Map<String, RateLimiter> cityRateLimits = Collections.synchronizedMap(new HashMap<>());

    @AfterReturning(pointcut = "execution(* se.cockroachdb.ledger.service.transfer.DefaultTransferService.createTransfer(..))",
            returning = "transfer")
    public void doAfterTransfer(Transfer transfer) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected existing transaction - check advisor @Order");

        TransferCreatedEvent event = new TransferCreatedEvent(this);
        event.setId(transfer.getId());
        event.setCity(transfer.getCity());

        // Throttle events to pass through every 5th second and discard the rest
        RateLimiter rateLimiter = cityRateLimits.computeIfAbsent(
                transfer.getCity(), o -> RateLimiter.create(1 / 5.0));
        if (rateLimiter.tryAcquire()) {
            // Defer transfer city account summary query and STOMP message
            applicationEventPublisher.publishEvent(event);
        }
    }

    @EventListener
    public void handle(TransferCreatedEvent event) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected no transaction - check advisor @Order");

        CityModel cityModel = new CityModel();
        cityModel.setName(event.getCity());

        reportingRepository.accountSummary(event.getCity()).ifPresent(accountSummary -> {
            cityModel.setUpdatedAt(accountSummary.getUpdatedAt());
            cityModel.setNumberOfAccounts(accountSummary.getNumberOfAccounts());
            cityModel.setMinBalance(accountSummary.getMaxBalance());
            cityModel.setMaxBalance(accountSummary.getMaxBalance());
            cityModel.setTotalBalance(accountSummary.getTotalBalance());
        });

        reportingRepository.transactionSummary(event.getCity()).ifPresent(transferSummary -> {
            cityModel.setNumberOfTransfers(transferSummary.getNumberOfTransfers());
            cityModel.setNumberOfLegs(transferSummary.getNumberOfLegs());
            cityModel.setTotalTurnover(transferSummary.getTotalTurnover());
        });

        simpMessagePublisher.convertAndSend(TopicName.REGION_CITY_UPDATE, cityModel);
    }
}
