package io.cockroachdb.ledger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
public class Application {
    private static void printHelpAndExit(String message) {
        System.out.println("Usage: java --jar ledger.jar <options> [args..]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("--help                    this help");
        System.out.println("--profiles [profile,..]   spring profiles to activate");
        System.out.println("  Available profiles:");
        System.out.println("    default *) - use account plan and region definitions in 'config/application-default.yml'.");
        System.out.println("    test    *) - use account plan and region definitions in 'config/application-test.yml'.");
        System.out.println("    outbox     - enable writing to outbox table for CDC.");
        System.out.println("    retries    - enable aspect-oriented transaction retries.");
        System.out.println("    jpa        - enable JPA and Hibernate repositories.");
        System.out.println();
        System.out.println("*) Pick either one or another named profile in 'config/' directory. Default is used when empty.");
        System.out.println();
        System.out.println("All other options are passed directly to spring container.");
        System.out.println();
        System.out.println(message);

        System.exit(0);
    }

    public static void main(String[] args) {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThroughArgs = new LinkedList<>();

        Set<String> profiles =
                StringUtils.commaDelimitedListToSet(System.getProperty("spring.profiles.active"));

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--help")) {
                printHelpAndExit("");
            } else if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printHelpAndExit("Expected list of profile names");
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else {
                passThroughArgs.add(arg);
            }
        }

        if (!profiles.isEmpty()) {
            System.setProperty("spring.profiles.active", String.join(",", profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.SERVLET)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[]{}));
    }
}
