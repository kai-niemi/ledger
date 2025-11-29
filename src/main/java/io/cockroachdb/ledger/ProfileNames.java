package io.cockroachdb.ledger;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Definition of Spring profile names for the application domain.
 */
public abstract class ProfileNames {
    public static final String DEV = "dev";

    public static final String JPA = "jpa";

    public static final String NOT_JPA = "!jpa";

    public static final String OUTBOX = "outbox";

    public static final String RETRIES = "retries";

    private ProfileNames() {
    }

    public static boolean acceptsPostgresSQL(Environment environment) {
        return environment.acceptsProfiles(Profiles.of("psql"));
    }
}
