package io.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.BeanListTableModel;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.TableUtils;

@Component
public class AccountReportCommands extends AbstractShellCommand {
    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "List accounts",
            name = {"report", "accounts"},
            completionProvider = "accountTypeProvider",
            group = Constants.REPORTING_COMMANDS)
    public void listAccounts(@Option(description = "account type",
                                     defaultValue = "LIABILITY",
                                     longName = "accountType") AccountType accountType,
                             @Option(description = "page size", defaultValue = "10",
                                     longName = "pageSize") Integer pageSize) {
        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<AccountEntity> accountPage = accountServiceFacade.findAccounts(accountType, page);
            logger.info("\n" + printAccountTable(accountPage.getContent()));
            page = askForPage(accountPage).orElseGet(Pageable::unpaged);
        }
    }

    private static String printAccountTable(List<AccountEntity> accountEntities) {
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
}

