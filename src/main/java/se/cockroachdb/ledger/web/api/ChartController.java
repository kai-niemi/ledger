package se.cockroachdb.ledger.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import se.cockroachdb.ledger.workload.Workload;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.util.metrics.Metrics;
import se.cockroachdb.ledger.util.metrics.TimeSeries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Chart JS data paint callback methods.
 */
@RestController
@RequestMapping(value = "/api/chart")
public class ChartController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    @Qualifier("threadPoolTimeSeries")
    private TimeSeries threadPoolTimeSeries;

    @Autowired
    @Qualifier("connectionPoolTimeSeries")
    private TimeSeries connectionPoolTimeSeries;

    @Autowired
    @Qualifier("cpuTimeSeries")
    private TimeSeries cpuTimeSeries;

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void takeDataPointSnapshots() {
        workloadManager.takeSnapshot();
        threadPoolTimeSeries.takeSnapshot();
        connectionPoolTimeSeries.takeSnapshot();
        cpuTimeSeries.takeSnapshot();
    }

    @GetMapping(value = "/data-points/connection-pool",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolDataPoints() {
        return connectionPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/data-points/thread-pool",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getThreadPoolDataPoints() {
        return threadPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/data-points/cpu",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getCpuDataPoints() {
        return cpuTimeSeries.getDataPoints();
    }

    @GetMapping(value = "/data-points/workloads/p99",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP99(Pageable page) {
//        logger.info("Get workload P99 page: " + page);
        return workloadManager.getDataPoints(Metrics::getP99, page);
    }

    @GetMapping(value = "/data-points/workloads/tps",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsTPS(Pageable page) {
//        logger.info("Get workload TPS page: " + page);
        return workloadManager.getDataPoints(Metrics::getOpsPerSec, page);
    }

    @GetMapping("/workloads/items")
    public @ResponseBody List<Workload> getWorkloadItems(Pageable page) {
//        logger.info("Get workload items page: " + page);
        return workloadManager.getWorkloads(page, (x) -> true).getContent();
    }

    @GetMapping("/workloads/summary")
    public @ResponseBody Metrics getWorkloadSummary(Pageable page) {
//        logger.info("Get workload items page: " + page);
        return workloadManager.getMetricsAggregate(page);
    }

}
