package se.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import se.cockroachdb.ledger.domain.AccountEntity;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.service.AccountServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.TableUtils;

@ShellComponent
@ShellCommandGroup(Constants.REPORT_COMMANDS)
public class AccountReportCommands extends AbstractInteractiveCommand {
    public static String printAccountTable(List<AccountEntity> accountEntities) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id", "Id");
        header.put("name", "Name");
        header.put("city", "City");
        header.put("accountType", "Type");
        header.put("balance", "Balance");
        header.put("allowNegative", "Allow Negative");
        header.put("closed", "Closed");

        return TableUtils.prettyPrint(new BeanListTableModel<>(accountEntities, header));
    }

    @Autowired
    private AccountServiceFacade accountServiceFacade;

    @ShellMethod(value = "List accounts", key = {"list-accounts", "la"})
    public void listAccounts(@ShellOption(help = "account type",
                                     defaultValue = "LIABILITY",
                                     valueProvider = EnumValueProvider.class) AccountType accountType,
                             @ShellOption(help = "page size", defaultValue = "10") Integer pageSize) {
        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<AccountEntity> accountPage = accountServiceFacade.findAccounts(accountType, page);
            logger.info("\n" + printAccountTable(accountPage.getContent()));
            page = askForPage(accountPage).orElseGet(Pageable::unpaged);
        }
    }
}

