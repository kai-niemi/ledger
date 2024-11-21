package se.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import se.cockroachdb.ledger.workload.Workload;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.shell.support.ListTableModel;
import se.cockroachdb.ledger.shell.support.TableUtils;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_ADMIN_COMMANDS)
public class WorkloadCommands extends AbstractInteractiveCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Cancel all workloads", key = {"cancel-all", "x"})
    public void cancelAll() {
        workloadManager.cancelWorkloads();
    }

    @ShellMethod(value = "Cancel one workload", key = {"cancel", "c"})
    public void cancel(@ShellOption(help = "workload id") Integer id) {
        workloadManager.cancelWorkload(id);
    }

    @ShellMethod(value = "Delete all workloads", key = {"delete-all", "da"})
    public void deleteWorkloads() {
        workloadManager.deleteWorkloads();
    }

    @ShellMethod(value = "Delete one workload", key = {"delete", "d"})
    public void delete(@ShellOption(help = "workload id") Integer id) {
        workloadManager.deleteWorkload(id);
    }

    @ShellMethod(value = "List all running workloads", key = {"list-workloads", "lw"},
            group = Constants.WORKLOAD_ADMIN_COMMANDS)
    public void listWorkloads(@ShellOption(help = "page size", defaultValue = "10") Integer pageSize) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id", "Id");
        header.put("title", "Title");
        header.put("category", "Category");
        header.put("executionTime", "Time");
        header.put("metrics.opsPerSec", "op/s");
        header.put("metrics.p90", "P90 (ms)");
        header.put("metrics.p99", "P99 (ms)");
        header.put("metrics.success", "Success");
        header.put("metrics.transientFail", "Transient");
        header.put("metrics.nonTransientFail", "Non-Transient");

        Pageable page = PageRequest.ofSize(pageSize);

        while (page.isPaged()) {
            Page<Workload> workloadPage = workloadManager.getWorkloads(page, Workload::isRunning);

            logger.info("\n" + TableUtils.prettyPrint(
                    new BeanListTableModel<>(workloadPage.getContent(), header)));

            page = askForPage(workloadPage).orElseGet(Pageable::unpaged);
        }
    }

    @ShellMethod(value = "List workload errors", key = {"list-errors", "le"},
            group = Constants.WORKLOAD_ADMIN_COMMANDS)
    public void listErrors(@ShellOption(help = "workload id") Integer id) {
        Workload workloadModel = workloadManager.getWorkloadById(id);
        List<Throwable> errors = workloadModel.getLastErrors();

        AtomicInteger idx = new AtomicInteger();
        logger.info("\n" + TableUtils.prettyPrint(
                new ListTableModel<>(errors,
                        List.of("#", "Type", "Message", "Cause"), (object, column) -> {
                    return switch (column) {
                        case 0 -> idx.incrementAndGet();
                        case 1 -> object.getClass().getSimpleName();
                        case 2 -> object.getMessage();
                        case 3 -> object.getCause();
                        default -> "??";
                    };
                })));
    }
}
