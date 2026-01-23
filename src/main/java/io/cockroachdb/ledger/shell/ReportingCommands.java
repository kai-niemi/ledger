package io.cockroachdb.ledger.shell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.BeanListTableModel;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferItemEntity;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.model.BalanceSheet;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.service.ReportingServiceFacade;
import io.cockroachdb.ledger.service.TransferServiceFacade;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.TableUtils;
import io.cockroachdb.ledger.util.AsciiArt;

@Component
public class ReportingCommands extends AbstractShellCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReportingServiceFacade reportingServiceFacade;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @Autowired
    private TransferServiceFacade transferServiceFacade;

    @Command(description = "Print balance sheets grouped by city",
            name = {"report", "account", "balance"},
            completionProvider = "regionCompletionProvider",
            group = Constants.REPORTING_COMMANDS)
    public void printBalanceSheets(@Option(description = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION, required = true,
            longName = "region") String region
    ) {
        Set<City> cities = regionServiceFacade.listCities(region);

        List<BalanceSheet> balanceSheets = reportingServiceFacade.getBalanceSheets(cities);

        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("city.name", "City");
        header.put("city.country", "Country");
        header.put("minBalance", "Min Balance");
        header.put("maxBalance", "Max Balance");
        header.put("totalBalance", "Total Balance");
        header.put("lastActive", "Last Active");
        header.put("numberOfTransfers", "Transfer #");
        header.put("numberOfLegs", "Leg #");
        header.put("totalChecksum", "Checksum");
        header.put("totalTurnover", "Turnover");

        logger.info("\n" + TableUtils.prettyPrint(new BeanListTableModel<>(balanceSheets, header)));
    }

    @Command(description = "Run consistency check on all accounts and transfers",
            name = {"report", "consistency"},
            completionProvider = "regionCompletionProvider",
            group = Constants.REPORTING_COMMANDS)
    public void printConsistencyReport(@Option(description = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION, required = true,
            longName = "region") String region
    ) {
        Set<City> cities = regionServiceFacade.listCities(region);
        List<BalanceSheet> balanceSheets = reportingServiceFacade.getBalanceSheets(cities);

        List<String> anomalies = new ArrayList<>();

        balanceSheets.forEach(balanceSheet -> {
            if (!balanceSheet.getTotalBalance().isZero()) {
                anomalies.add("Non-zero total balance %s for city %s"
                        .formatted(balanceSheet.getTotalBalance(),
                                balanceSheet.getCity().getName()));
            }
            if (!balanceSheet.getTotalChecksum().isZero()) {
                anomalies.add("Non-zero transfer leg checksum %s for city %s."
                        .formatted(balanceSheet.getTotalChecksum(),
                                balanceSheet.getCity().getName()));
            }
        });

        logger.info("Database version: " + regionServiceFacade.getDatabaseVersion());
        logger.info("Transaction isolation: " + regionServiceFacade.getDatabaseIsolation());

        if (anomalies.isEmpty()) {
            logger.info("No anomalies detected! " + AsciiArt.happy());
        } else {
            logger.warn("%d anomalies detected! You are a victim of a circumstance, or P4 lost update! %s"
                    .formatted(anomalies.size(), AsciiArt.flipTableRoughly()));
            anomalies.forEach(logger::error);
        }
    }


    @Command(description = "List transfer transactions",
            name = {"report", "transfer"},
            completionProvider = "transferTypeProvider",
            group = Constants.REPORTING_COMMANDS)
    public void listTransfers(
            @Option(description = "transfer type", defaultValue = "BANK", required = true,
                    longName = "transferType") TransferType transferType,
            @Option(description = "page size", defaultValue = "10",
                    longName = "pageSize") Integer pageSize) {

        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<TransferEntity> transferPage = transferServiceFacade.findTransfers(transferType, page);
            logger.info("\n" + printTransferTable(transferPage.getContent()));
            page = askForPage(transferPage).orElseGet(Pageable::unpaged);
        }
    }

    @Command(description = "List transfer transaction legs",
            name = {"report", "transfer", "legs"},
            group = Constants.REPORTING_COMMANDS)
    public void listTransferItems(@Option(description = "transfer id", required = true,
                                              longName = "id") UUID id,
                                  @Option(description = "page size", defaultValue = "10",
                                          longName = "pageSize") Integer pageSize) {
        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<TransferItemEntity> transferItemPage = transferServiceFacade.findTransferItems(id, page);
            logger.info("\n" + printTransferItemsTable(transferItemPage.getContent()));
            page = askForPage(transferItemPage).orElseGet(Pageable::unpaged);
        }
    }

    public static String printTransferTable(List<TransferEntity> transferEntities) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id", "Id");
        header.put("city", "City");
        header.put("transferType", "Type");
        header.put("transferDate", "Transfer Date");
        header.put("bookingDate", "Booking Date");

        return TableUtils.prettyPrint(new BeanListTableModel<>(transferEntities, header));
    }

    public static String printTransferItemsTable(List<TransferItemEntity> items) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id.accountId", "Account Id");
        header.put("id.itemPos", "#");
        header.put("city", "City");
        header.put("amount", "Amount");
        header.put("runningBalance", "Running Balance");
        header.put("note", "Note");

        return TableUtils.prettyPrint(new BeanListTableModel<>(items, header));
    }
}
