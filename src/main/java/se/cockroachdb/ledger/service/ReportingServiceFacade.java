package se.cockroachdb.ledger.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cockroachdb.ledger.annotations.ServiceFacade;
import se.cockroachdb.ledger.annotations.TransactionImplicit;
import se.cockroachdb.ledger.model.AccountSummary;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.model.TransferSummary;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.util.ConcurrencyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@ServiceFacade
public class ReportingServiceFacade {
    @Autowired
    private ReportingRepository reportingRepository;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @TransactionImplicit(readOnly = true)
    public List<AccountSummary> getAccountSummaryByRegion(String region) {
        List<Region> regions = regionServiceFacade.listRegions(region);
        return getAccountSummary(Region.joinCities(regions));
    }

    @TransactionImplicit(readOnly = true)
    public List<AccountSummary> getAccountSummary(List<City> cities) {
        final List<Callable<AccountSummary>> tasks = new ArrayList<>();

        cities.forEach(c -> tasks.add(() -> {
            return reportingRepository.accountSummary(c.getName())
                    .orElse(AccountSummary.empty(c));
        }));

        final List<AccountSummary> allSummaries = new ArrayList<>();

        ConcurrencyUtils.runConcurrentlyAndWait(tasks,
                ConcurrencyUtils.UNBOUNDED_CONCURRENCY, allSummaries::add);

        return allSummaries;
    }

    @TransactionImplicit(readOnly = true)
    public List<TransferSummary> getTransactionSummaryByRegion(String region) {
        List<Region> regions = regionServiceFacade.listRegions(region);
        return getTransactionSummary(Region.joinCities(regions));
    }

    @TransactionImplicit(readOnly = true)
    public List<TransferSummary> getTransactionSummary(List<City> cities) {
        final List<Callable<TransferSummary>> tasks = new ArrayList<>();

        cities.forEach(c -> tasks.add(() -> {
            return reportingRepository.transactionSummary(c.getName())
                    .orElse(TransferSummary.empty(c));
        }));

        final List<TransferSummary> allSummaries = new ArrayList<>();

        ConcurrencyUtils.runConcurrentlyAndWait(tasks,
                ConcurrencyUtils.UNBOUNDED_CONCURRENCY, allSummaries::add);

        return allSummaries;
    }
}
