package io.cockroachdb.ledger.web.front;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import io.cockroachdb.ledger.push.SimpMessagePublisher;
import io.cockroachdb.ledger.push.TopicName;
import io.cockroachdb.ledger.web.model.WorkloadForm;
import io.cockroachdb.ledger.service.workload.Workload;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.service.workload.WorkloadUpdatedEvent;

@Controller
@RequestMapping("/workload")
public class WorkloadController {
    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private SimpMessagePublisher messagePublisher;

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void modelUpdate() {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_MODEL_UPDATE, null);
    }

    @EventListener
    public void handle(WorkloadUpdatedEvent event) {
        messagePublisher.convertAndSendThrottled(TopicName.WORKLOAD_REFRESH_PAGE,
                null, "workloads", .25); // every 4:th sec
    }

    @GetMapping
    public Callable<String> listWorkloads(
            @RequestParam(value = "type", required = false) String type,
            @PageableDefault(size = 10) Pageable page,
            Model model) {
        Page<Workload> workloadPage = workloadManager.getWorkloads(page, workload -> {
            return type == null || workload.getTitle().equalsIgnoreCase(type);
        });

        Set<String> allTitles = workloadManager.getWorkloads()
                .stream().map(Workload::getTitle)
                .collect(Collectors.toSet());
        allTitles.add("");

        model.addAttribute("workloadPage", workloadPage);
        model.addAttribute("form", new WorkloadForm(type));
        model.addAttribute("workloadTitles", allTitles);
        model.addAttribute("aggregatedMetrics", workloadManager.getMetricsAggregate(page));

        return () -> "workload";
    }

    @PostMapping
    public Callable<String> filterWorkloads(@ModelAttribute("form") WorkloadForm form,
                                            @PageableDefault(size = 10) Pageable page,
                                            Model model) {
        return () -> {
            Page<Workload> workloadPage = workloadManager.getWorkloads(page, workload -> {
                return workload.getTitle().equalsIgnoreCase(form.getTitle());
            });

            Set<String> allTitles = workloadManager.getWorkloads()
                    .stream().map(Workload::getTitle).collect(Collectors.toSet());
            allTitles.add("");

            model.addAttribute("workloadPage", workloadPage);
            model.addAttribute("form", form);
            model.addAttribute("workloadTitles", allTitles);
            model.addAttribute("aggregatedMetrics", workloadManager.getMetricsAggregate(page));

            return "workload";
        };
    }

    @GetMapping("{id}")
    public Callable<String> workloadDetails(
            @PathVariable("id") Integer id,
            Model model) {
        return () -> {
            Workload workload = workloadManager.getWorkloadById(id);

            model.addAttribute("form", workload);

            return "workload-detail";
        };
    }


    @PostMapping(value = "/cancelAll")
    public RedirectView cancelAll() {
        workloadManager.cancelWorkloads();
        return new RedirectView("/workload");
    }

    @PostMapping(value = "/deleteAll")
    public RedirectView deleteAll() {
        workloadManager.deleteWorkloads();
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/cancel/{id}")
    public RedirectView cancel(@PathVariable("id") Integer id) {
        Workload workload = workloadManager.getWorkloadById(id);
        workload.cancel();
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/delete/{id}")
    public RedirectView delete(@PathVariable("id") Integer id) {
        workloadManager.deleteWorkload(id);
        return new RedirectView("/workload");
    }

    @GetMapping("/data-points/clear")
    public RedirectView clearDataPoints() {
        workloadManager.clearDataPoints();
        return new RedirectView("/workload");
    }
}
