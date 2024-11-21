package se.cockroachdb.ledger.web.front;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import se.cockroachdb.ledger.event.WorkloadUpdatedEvent;
import se.cockroachdb.ledger.workload.Workload;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.web.push.SimpMessagePublisher;
import se.cockroachdb.ledger.web.push.TopicName;

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

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void chartUpdate() {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_CHARTS_UPDATE, null);
    }

    @EventListener
    public void handle(WorkloadUpdatedEvent event) {
        messagePublisher.convertAndSendThrottled(TopicName.WORKLOAD_REFRESH_PAGE,
                null, "workloads", .25);
    }

    @GetMapping
    public Callable<String> indexPage(Model model, @PageableDefault(size = 10) Pageable page) {
        model.addAttribute("workloadPage",
                workloadManager.getWorkloads(page, (x) -> true));
        model.addAttribute("aggregatedMetrics",
                workloadManager.getMetricsAggregate(page));
        return () -> "workload";
    }

    @GetMapping("/detail/{id}")
    public Callable<String> workloadDetails(@PathVariable("id") Integer id, Model model) {
        return () -> "workload";
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
