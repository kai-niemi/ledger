package se.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import se.cockroachdb.ledger.model.AccountSummary;
import se.cockroachdb.ledger.model.TransferSummary;
import se.cockroachdb.ledger.service.ReportingServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.shell.support.TableUtils;

@ShellComponent
@ShellCommandGroup(Constants.REPORT_COMMANDS)
public class ReportCommands extends AbstractShellComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReportingServiceFacade reportingServiceFacade;

    @ShellMethod(value = "Print account summary", key = {"account-summary", "as"})
    public void accountSummary(@ShellOption(help = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION,
            valueProvider = RegionProvider.class) String region
    ) {
        List<AccountSummary> summary = reportingServiceFacade.getAccountSummaryByRegion(region);

        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("city", "City");
        header.put("minBalance", "Min Balance");
        header.put("maxBalance", "Max Balance");
        header.put("totalBalance", "Total Balance");
        header.put("lastActive", "Last Active");

        logger.info("\n" + TableUtils.prettyPrint(
                new BeanListTableModel<>(summary, header)));
    }

    @ShellMethod(value = "Print transfer summary", key = {"transfer-summary", "ts"})
    public void transferSummary(@ShellOption(help = Constants.REGIONS_HELP,
            defaultValue = Constants.DEFAULT_REGION,
            valueProvider = RegionProvider.class) String region
    ) {
        List<TransferSummary> summaries = reportingServiceFacade.getTransactionSummaryByRegion(region);

        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("city", "City");
        header.put("numberOfTransfers", "Transfers");
        header.put("numberOfLegs", "Legs");
        header.put("totalCheckSum", "Checksum");
        header.put("totalTurnover", "Turnover");

        logger.info("\n" + TableUtils.prettyPrint(
                new BeanListTableModel<>(summaries, header)));
    }

}
