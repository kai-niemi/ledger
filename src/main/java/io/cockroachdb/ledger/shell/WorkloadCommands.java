package io.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

import io.cockroachdb.ledger.service.workload.Problem;
import io.cockroachdb.ledger.service.workload.Workload;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.ListTableModel;
import io.cockroachdb.ledger.shell.support.TableUtils;

@Component
public class WorkloadCommands extends AbstractShellCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WorkloadManager workloadManager;

    @Command(description = "Cancel all workloads",
            name = {"workload", "cancel", "all"},
            alias = "x",
            group = Constants.WORKLOAD_COMMANDS)
    public void cancelAll() {
        workloadManager.cancelWorkloads();
    }

    @Command(description = "Cancel one workload",
            name = {"workload", "cancel", "one"},
            group = Constants.WORKLOAD_COMMANDS)
    public void cancel(@Option(description = "workload id", required = true,
            longName = "id") Integer id) {
        workloadManager.cancelWorkload(id);
    }

    @Command(description = "Delete all non-running workloads",
            name = {"workload", "delete", "all"},
            group = Constants.WORKLOAD_COMMANDS)
    public void deleteWorkloads() {
        workloadManager.deleteWorkloads();
    }

    @Command(description = "Delete one workload",
            name = {"workload", "delete", "one"},
            group = Constants.WORKLOAD_COMMANDS)
    public void delete(@Option(description = "workload id", required = true,
            longName = "id") Integer id) {
        workloadManager.deleteWorkload(id);
    }

    @Command(description = "List all running workloads",
            name = {"workload", "list"},
            group = Constants.WORKLOAD_COMMANDS)
    public void listWorkloads(@Option(description = "page size", defaultValue = "20",
            longName = "pageSize") Integer pageSize) {
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

    @Command(description = "List workload errors",
            name = {"workload", "list", "errors"},
            group = Constants.WORKLOAD_COMMANDS)
    public void listErrors(@Option(description = "workload id", required = true,
            longName = "id") Integer id) {
        Workload workloadModel = workloadManager.getWorkloadById(id);
        List<Problem> problems = workloadModel.getLastProblems();

        AtomicInteger idx = new AtomicInteger();
        logger.info("\n" + TableUtils.prettyPrint(
                new ListTableModel<>(problems,
                        List.of("#", "Type", "Message", "Cause"), (object, column) -> {
                    return switch (column) {
                        case 0 -> idx.incrementAndGet();
                        case 1 -> object.getClassName();
                        case 2 -> object.getMessage();
                        case 3 -> object.getStackTrace();
                        default -> "??";
                    };
                })));
    }
}
