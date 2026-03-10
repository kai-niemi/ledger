package io.cockroachdb.ledger.shell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.shell.jline.tui.table.BeanListTableModel;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.service.workload.Problem;
import io.cockroachdb.ledger.service.workload.Workload;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.service.workload.WorkloadStatus;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.ListTableModel;
import io.cockroachdb.ledger.shell.support.TableUtils;

@Component
public class WorkloadAdminCommands extends AbstractShellCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @Bean
    public CompletionProvider workloadProvider() {
        return completionContext -> {
            List<CompletionProposal> result = new ArrayList<>();

            workloadManager.getWorkloads().forEach(workload -> {
                CompletionProposal p = new CompletionProposal("--id=" + workload.getId())
                        .description(workload.getTitle());
                result.add(0, p);
            });

            return result;
        };
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Cancel workload(s)",
            name = {"workload", "cancel"},
            alias = "x",
            completionProvider = "workloadProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void cancelAll(@Option(description = "workload id", longName = "id", defaultValue = "-1") Integer id) {
        if (id >= 0) {
            workloadManager.cancelWorkload(id);
        } else {
            workloadManager.cancelWorkloads();
        }
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Delete workload(s)",
            name = {"workload", "delete"},
            completionProvider = "workloadProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void delete(@Option(description = "workload id", defaultValue = "-1",
            longName = "id") Integer id) {
        if (id >= 0) {
            workloadManager.deleteWorkload(id);
        } else {
            workloadManager.deleteWorkloads();
        }
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Print workload summary",
            name = {"workload", "summary"},
            alias = "y",
            group = Constants.WORKLOAD_COMMANDS)
    public void workloadSummary(CommandContext commandContext) {
        final Map<String, List<Workload>> all = new LinkedHashMap<>();
        final Map<WorkloadStatus, AtomicInteger> status = new LinkedHashMap<>();
        final AtomicInteger total = new AtomicInteger();

        workloadManager.getWorkloads().forEach(workload -> {
            all.computeIfAbsent(workload.getCategory(), x -> new ArrayList<>()).add(workload);
        });

        if (all.isEmpty()) {
            commandContext.outputWriter().println("No workloads");
            return;
        }

        commandContext.outputWriter().println("Workload summary:");

        all.forEach((category, workloads) -> {
            commandContext.outputWriter().println("%s (%d)".formatted(category, workloads.size()));
            total.addAndGet(workloads.size());

            workloads.forEach(workload ->
                    status.computeIfAbsent(workload.getStatus(), x -> new AtomicInteger()).incrementAndGet());
        });

        commandContext.outputWriter().println("Total workloads: %d".formatted(total.get()));

        commandContext.outputWriter().println("Status summary:");
        status.forEach((workloadStatus, x) -> {
            commandContext.outputWriter().println("%s (%d)".formatted(workloadStatus, x.get()));
        });
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "List all workloads",
            name = {"workload", "list"},
            alias = "w",
            completionProvider = "workloadStatusProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void listWorkloads(@Option(description = "page size", defaultValue = "20",
                                      longName = "pageSize") Integer pageSize,
                              @Option(description = "workload status", defaultValue = "RUNNING",
                                      longName = "status") WorkloadStatus status,
                              CommandContext commandContext) {
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
            Page<Workload> workloadPage = workloadManager.getWorkloads(page, workload ->
                    workload.getStatus().equals(status));

            commandContext.outputWriter().println(TableUtils.prettyPrint(
                    new BeanListTableModel<>(workloadPage.getContent(), header)));

            page = askForPage(workloadPage).orElseGet(Pageable::unpaged);
        }
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "List all errors",
            name = {"workload", "errors"},
            completionProvider = "workloadProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void listErrors(@Option(description = "workload id", longName = "id", defaultValue = "-1") Integer id,
                           CommandContext commandContext) {
        List<Problem> problems = new ArrayList<>();

        if (id >= 0) {
            Workload workloadModel = workloadManager.getWorkloadById(id);
            problems.addAll(workloadModel.getLastProblems());
        } else {
            workloadManager.getWorkloads().forEach(workload -> {
                problems.addAll(workload.getLastProblems());
            });
        }

        AtomicInteger idx = new AtomicInteger();
        commandContext.outputWriter().println(
                TableUtils.prettyPrint(
                        new ListTableModel<>(problems,
                                List.of("#", "Title", "Type", "Message", "Cause"), (object, column) -> {
                            return switch (column) {
                                case 0 -> idx.incrementAndGet();
                                case 1 -> object.getTitle();
                                case 2 -> object.getClassName();
                                case 3 -> object.getMessage();
                                case 4 -> object.getStackTrace();
                                default -> "??";
                            };
                        })));
    }
}
