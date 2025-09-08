package io.cockroachdb.ledger.shell.support;

public abstract class Constants {
    public static final String ADMIN_COMMANDS = "1) Admin Commands";

    public static final String CONNECTION_POOL_COMMANDS = "2) Connection Pool Commands";

    public static final String REGION_QUERY_COMMANDS = "3) Region Query Commands";

    public static final String REGION_MODIFICATION_COMMANDS = "4) Region Modification Commands";

    public static final String REPORT_COMMANDS = "5) Reporting Commands";

    public static final String PLAN_COMMANDS = "6) Account Plan Commands";

    public static final String WORKLOAD_QUERY_COMMANDS = "7) Workload Query Commands";

    public static final String WORKLOAD_MODIFICATION_COMMANDS = "8) Workload Modification Commands";

    public static final String WORKLOAD_START_COMMANDS = "9) Workload Commands";

    public static final String DEFAULT_DURATION = "120m";

    public static final String DURATION_HELP = "Workload execution duration";

    public static final String ITERATIONS_HELP = "Max execution iterations (overrides duration if > 0)";

    public static final String REGIONS_HELP = "Name of account region(s) to include in city selection."
                                              + "\n'GATEWAY' refers to CockroachDB gateway node region."
                                              + "\n'PRIMARY' refers to CockroachDB primary node region."
                                              + "\n'SECONDARY' refers to CockroachDB secondary node region."
                                              + "\n'ALL' includes all regions.";

    public static final String ACCOUNT_LIMIT_HELP = "Max number of accounts per city";

    public static final String DEFAULT_ACCOUNT_LIMIT = "5000";

    public static final String DEFAULT_REGION = "GATEWAY";

    private Constants() {
    }
}
