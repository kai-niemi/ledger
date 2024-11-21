package se.cockroachdb.ledger.util;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DurationUtils {
    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)([smhdw])");

    private DurationUtils() {
    }

    public static Duration parseDuration(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration.toLowerCase(Locale.ENGLISH));
        Duration instant = Duration.ZERO;
        while (matcher.find()) {
            int ordinal = Integer.parseInt(matcher.group(1));
            String token = matcher.group(2);
            switch (token) {
                case "s":
                    instant = instant.plus(Duration.ofSeconds(ordinal));
                    break;
                case "m":
                    instant = instant.plus(Duration.ofMinutes(ordinal));
                    break;
                case "h":
                    instant = instant.plus(Duration.ofHours(ordinal));
                    break;
                case "d":
                    instant = instant.plus(Duration.ofDays(ordinal));
                    break;
                case "w":
                    instant = instant.plus(Duration.ofDays(ordinal * 7L));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid token " + token);
            }
        }
        if (instant.equals(Duration.ZERO)) {
            return Duration.ofSeconds(Integer.parseInt(duration));
        }
        return instant;
    }

    public static String durationToDisplayString(Duration duration) {
        return millisecondsToDisplayString(duration.toMillis());
    }

    public static String millisecondsToDisplayString(long timeMillis) {
        double seconds = (timeMillis / 1000.0) % 60;
        int minutes = (int) ((timeMillis / 60000) % 60);
        int hours = (int) ((timeMillis / 3600000));

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(String.format("%dh", hours));
        }
        if (hours > 0 || minutes > 0) {
            sb.append(String.format("%dm", minutes));
        }
        if (hours == 0 && seconds > 0) {
            sb.append(String.format(Locale.US, "%.1fs", seconds));
        }
        return sb.toString();
    }

    public static <V> Duration executionDuration(Callable<V> task) {
        try {
            Instant start = Instant.now();
            task.call();
            return Duration.between(start, Instant.now());
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
