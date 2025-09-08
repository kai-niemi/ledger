package io.cockroachdb.ledger.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;

import io.cockroachdb.ledger.annotations.ServiceFacade;
import io.cockroachdb.ledger.annotations.TransactionImplicit;
import io.cockroachdb.ledger.domain.AccountSummary;
import io.cockroachdb.ledger.domain.TransferSummary;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.repository.ReportingRepository;
import io.cockroachdb.ledger.model.BalanceSheet;

@ServiceFacade
public class ReportingServiceFacade {
    @Autowired
    private ReportingRepository reportingRepository;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;

    @TransactionImplicit(readOnly = true)
    public List<BalanceSheet> getBalanceSheets(Set<City> cities) {
        List<CompletableFuture<BalanceSheet>> allFutures = new ArrayList<>();

        cities.forEach(city -> {
            CompletableFuture<AccountSummary> f1 = asyncTaskExecutor.submitCompletable(() -> {
                return reportingRepository.accountSummary(city);
            });

            CompletableFuture<TransferSummary> f2 = asyncTaskExecutor.submitCompletable(() -> {
                return reportingRepository.transferSummary(city);
            });

            CompletableFuture<BalanceSheet> combinedFuture = f1.thenCombine(f2,
                    (accountSummary, transferSummary) -> {
                BalanceSheet balanceSheet = new BalanceSheet();
                balanceSheet.setCity(city);

                balanceSheet.setUpdatedAt(accountSummary.getUpdatedAt());
                balanceSheet.setNumberOfAccounts(accountSummary.getNumberOfAccounts());
                balanceSheet.setMinBalance(accountSummary.getMaxBalance());
                balanceSheet.setMaxBalance(accountSummary.getMaxBalance());
                balanceSheet.setTotalBalance(accountSummary.getTotalBalance());

                balanceSheet.setNumberOfTransfers(transferSummary.getNumberOfTransfers());
                balanceSheet.setNumberOfLegs(transferSummary.getNumberOfLegs());
                balanceSheet.setTotalTurnover(transferSummary.getTotalTurnover());
                balanceSheet.setTotalChecksum(transferSummary.getTotalCheckSum());

                return balanceSheet;
            });

            allFutures.add(combinedFuture);
        });

        final List<BalanceSheet> balanceSheets = new ArrayList<>();

        allFutures.forEach(balanceSheetCompletableFuture -> {
            balanceSheets.add(balanceSheetCompletableFuture.join());
        });

        return balanceSheets;
    }
}
