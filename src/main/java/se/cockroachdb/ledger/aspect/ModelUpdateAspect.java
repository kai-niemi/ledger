package se.cockroachdb.ledger.aspect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.util.concurrent.RateLimiter;

import se.cockroachdb.ledger.domain.AccountSummary;
import se.cockroachdb.ledger.domain.TransferEntity;
import se.cockroachdb.ledger.domain.TransferRequest;
import se.cockroachdb.ledger.domain.TransferSummary;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.model.BalanceSheet;
import se.cockroachdb.ledger.push.SimpMessagePublisher;
import se.cockroachdb.ledger.push.TopicName;

@Aspect
@Component
@Order(ModelUpdateAspect.PRECEDENCE)
public class ModelUpdateAspect {
    public static final int PRECEDENCE = AdvisorOrder.CHANGE_FEED_ADVISOR;

    @Autowired
    private SimpMessagePublisher simpMessagePublisher;

    @Autowired
    private ReportingRepository reportingRepository;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;

    private final Map<String, RateLimiter> rateLimiterMap = Collections.synchronizedMap(new HashMap<>());

    @AfterReturning(pointcut = "execution(* se.cockroachdb.ledger.service.transfer.DefaultTransferService.createTransfer(..)) "
                               + "&& args(transferRequest,..)",
            argNames = "transferRequest,returnedValue",
            returning = "returnedValue")
    public void doAfterTransfer(TransferRequest transferRequest, TransferEntity returnedValue) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected existing transaction - check advisor @Order");

        // Throttle events by discarding
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(returnedValue.getCity(),
                o -> RateLimiter.create(.25));
        if (rateLimiter.tryAcquire()) {
            // Defer queries and STOMP message
            asyncTaskExecutor.submitCompletable(() -> sendReport(transferRequest.getCity()));
        }
    }

    private void sendReport(City city) {
        BalanceSheet balanceSheet = new BalanceSheet();
        balanceSheet.setCity(city);

        AccountSummary accountSummary = reportingRepository.accountSummary(city);
        balanceSheet.setUpdatedAt(accountSummary.getUpdatedAt());
        balanceSheet.setNumberOfAccounts(accountSummary.getNumberOfAccounts());
        balanceSheet.setMinBalance(accountSummary.getMaxBalance());
        balanceSheet.setMaxBalance(accountSummary.getMaxBalance());
        balanceSheet.setTotalBalance(accountSummary.getTotalBalance());

        TransferSummary transferSummary = reportingRepository.transferSummary(city);
        balanceSheet.setNumberOfTransfers(transferSummary.getNumberOfTransfers());
        balanceSheet.setNumberOfLegs(transferSummary.getNumberOfLegs());
        balanceSheet.setTotalTurnover(transferSummary.getTotalTurnover());
        balanceSheet.setTotalChecksum(transferSummary.getTotalCheckSum());

        simpMessagePublisher.convertAndSend(TopicName.BALANCE_SHEET_UPDATE, balanceSheet);
    }
}
