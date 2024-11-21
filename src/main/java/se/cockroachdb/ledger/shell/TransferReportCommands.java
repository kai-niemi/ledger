package se.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.service.TransferServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.TableUtils;

@ShellComponent
@ShellCommandGroup(Constants.REPORT_COMMANDS)
public class TransferReportCommands extends AbstractInteractiveCommand {
    public static String printTransferTable(List<Transfer> transfers) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id", "Id");
        header.put("city", "City");
        header.put("transferType", "Type");
        header.put("transferDate", "Transfer Date");
        header.put("bookingDate", "Booking Date");

        return TableUtils.prettyPrint(new BeanListTableModel<>(transfers, header));
    }

    public static String printTransferItemsTable(List<TransferItem> items) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id.accountId", "Account Id");
        header.put("id.itemPos", "#");
        header.put("city", "City");
        header.put("amount", "Amount");
        header.put("runningBalance", "Running Balance");
        header.put("note", "Note");

        return TableUtils.prettyPrint(new BeanListTableModel<>(items, header));
    }

    @Autowired
    private TransferServiceFacade transferServiceFacade;

    @ShellMethod(value = "List transfers", key = {"list-transfers", "lt"})
    public void listTransfers(
//            @ShellOption(help = "transfer type",
//                    defaultValue = "BANK",
//                    valueProvider = EnumValueProvider.class) TransferType transferType,
            @ShellOption(help = "page size", defaultValue = "10") Integer pageSize) {

        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<Transfer> transferPage = transferServiceFacade.findTransfers(page);
            logger.info("\n" + printTransferTable(transferPage.getContent()));
            page = askForPage(transferPage).orElseGet(Pageable::unpaged);
        }
    }

    @ShellMethod(value = "List transfer items", key = {"list-transfer-items", "lti"})
    public void listTransferItems(@ShellOption(help = "transfer id") UUID id,
                                  @ShellOption(help = "page size", defaultValue = "10") Integer pageSize) {
        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            final Page<TransferItem> transferItemPage = transferServiceFacade.findTransferItems(id, page);
            logger.info("\n" + printTransferItemsTable(transferItemPage.getContent()));
            page = askForPage(transferItemPage).orElseGet(Pageable::unpaged);
        }
    }
}

