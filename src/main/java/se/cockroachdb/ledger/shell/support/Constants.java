package se.cockroachdb.ledger.shell.support;

public abstract class Constants {
    public static final String ADMIN_COMMANDS = "1) Admin Commands";

    public static final String CONNECTION_POOL_COMMANDS = "1.2) Connection Pool Commands";

    public static final String REGION_COMMANDS = "2) Region Commands";

    public static final String MULTI_REGION_COMMANDS = "2.1) Multi-Region Commands";

    public static final String REPORT_COMMANDS = "3) Reporting Commands";

    public static final String PLAN_COMMANDS = "4) Account Plan Commands";

    public static final String WORKLOAD_COMMANDS = "5) Workload Commands";

    public static final String WORKLOAD_ADMIN_COMMANDS = "5.1) Workload Admin Commands";

    public static final String DEFAULT_DURATION = "120m";

    public static final String DURATION_HELP = "Workload execution duration";

    public static final String ITERATIONS_HELP = "Max execution iterations (overrides duration if > 0)";

    public static final String REGIONS_HELP = "Name of account region(s) to include in city selection."
                                              + "\n'gateway' refers to CockroachDB gateway node region."
                                              + "\n'primary' refers to CockroachDB primary node region."
                                              + "\n'secondary' refers to CockroachDB secondary node region."
                                              + "\n'all' includes all regions.";

    public static final String CITY_NAME_HELP = "Narrow selection to one specific city (must exist in some region)";

    public static final String ACCOUNT_LIMIT_HELP = "Max number of accounts per city";

    public static final String DEFAULT_ACCOUNT_LIMIT = "5000";

    public static final String DEFAULT_REGION = "gateway";

    private Constants() {
    }
}
