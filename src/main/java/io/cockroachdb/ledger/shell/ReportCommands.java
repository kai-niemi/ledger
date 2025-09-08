package io.cockroachdb.ledger.shell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.service.ReportingServiceFacade;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.RegionProvider;
import io.cockroachdb.ledger.shell.support.TableUtils;
import io.cockroachdb.ledger.util.AsciiArt;
import io.cockroachdb.ledger.model.BalanceSheet;

@ShellComponent
@ShellCommandGroup(Constants.REPORT_COMMANDS)
public class ReportCommands extends AbstractShellComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReportingServiceFacade reportingServiceFacade;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @ShellMethod(value = "Print balance sheets grouped by city", key = {"balance-sheets", "bs"})
    public void printBalanceSheets(@ShellOption(help = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION,
            valueProvider = RegionProvider.class) String region
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

    @ShellMethod(value = "Run consistency check on all accounts and transfers",
            key = {"consistency-check", "cc"})
    public void printConsistencyReport(@ShellOption(help = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION,
            valueProvider = RegionProvider.class) String region
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
}
