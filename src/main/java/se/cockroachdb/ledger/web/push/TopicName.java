package se.cockroachdb.ledger.web.push;

public enum TopicName {
    REGION_REFRESH_PAGE("/topic/region/refresh"),
    REGION_TOAST_MESSAGE("/topic/region/toast"),
    REGION_CITY_UPDATE("/topic/region/city/update"),

    WORKLOAD_MODEL_UPDATE("/topic/workload/update"),
    WORKLOAD_CHARTS_UPDATE("/topic/workload/charts"),
    WORKLOAD_REFRESH_PAGE("/topic/workload/refresh"),

    METRIC_CHARTS_UPDATE("/topic/metric/charts"),
    METRIC_REFRESH_PAGE("/topic/metric/refresh");

    final String value;

    TopicName(java.lang.String value) {
        this.value = value;
    }
}
