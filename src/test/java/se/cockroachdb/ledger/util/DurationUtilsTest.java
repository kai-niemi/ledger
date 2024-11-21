package se.cockroachdb.ledger.util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DurationUtilsTest {

    @Test
    public void parseDurationExpressions() {
//        2020-09-03 07:03:22.165309+00:00
        System.out.println(LocalDateTime.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ")));

        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX").format(new Date()));

        Assertions.assertEquals(DurationUtils.parseDuration("30"), Duration.ofSeconds(30));
        Assertions.assertEquals(DurationUtils.parseDuration("30s"), Duration.ofSeconds(30));
        Assertions.assertEquals(DurationUtils.parseDuration("30m"), Duration.ofMinutes(30));
        Assertions.assertEquals(DurationUtils.parseDuration("30h"), Duration.ofHours(30));
        Assertions.assertEquals(DurationUtils.parseDuration("30d"), Duration.ofDays(30));

        Assertions.assertEquals(DurationUtils.parseDuration("10m30s"),
                Duration.ofMinutes(10).plus(Duration.ofSeconds(30)));
        Assertions.assertEquals(DurationUtils.parseDuration("10h3m15s"),
                Duration.ofHours(10).plus(Duration.ofMinutes(3).plus(Duration.ofSeconds(15))));
        Assertions.assertEquals(DurationUtils.parseDuration("10h 3m 15s"),
                Duration.ofHours(10).plus(Duration.ofMinutes(3).plus(Duration.ofSeconds(15))));
    }
}
