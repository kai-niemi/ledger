package io.cockroachdb.ledger.push;

public enum TopicName {
    WORKLOAD_MODEL_UPDATE("/topic/workload/update"),
    WORKLOAD_REFRESH_PAGE("/topic/workload/refresh"),

    BALANCE_SHEET_UPDATE("/topic/balance-sheet/update"),
    METRIC_CHARTS_UPDATE("/topic/metric/charts");

    final String value;

    TopicName(java.lang.String value) {
        this.value = value;
    }
}
