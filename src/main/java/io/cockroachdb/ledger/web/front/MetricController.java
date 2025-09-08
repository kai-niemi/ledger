package io.cockroachdb.ledger.web.front;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.cockroachdb.ledger.push.SimpMessagePublisher;
import io.cockroachdb.ledger.push.TopicName;

@Controller
@RequestMapping("/metric")
public class MetricController {
    @Autowired
    private SimpMessagePublisher messagePublisher;

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void chartUpdate() {
        messagePublisher.convertAndSend(TopicName.METRIC_CHARTS_UPDATE, null);
    }

    @GetMapping
    public Callable<String> indexPage(Model model) {
        return () -> "metric";
    }

}
