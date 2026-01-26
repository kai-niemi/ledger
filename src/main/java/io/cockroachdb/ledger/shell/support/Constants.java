package io.cockroachdb.ledger.shell.support;

public abstract class Constants {
    public static final String ADMIN_COMMANDS = "1) Admin Commands";

    public static final String DB_COMMANDS = "2) Database Commands";

    public static final String REGION_COMMANDS = "3) Region Commands";

    public static final String REPORTING_COMMANDS = "4) Reporting Commands";

    public static final String WORKLOAD_COMMANDS = "5) Workload Commands";

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
