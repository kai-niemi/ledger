package se.cockroachdb.ledger.shell.support;

import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import se.cockroachdb.ledger.util.DurationUtils;

@Component
public class AnsiConsole {
    private final Lock lock = new ReentrantLock();

    private final Terminal terminal;

    public AnsiConsole(@Autowired @Lazy Terminal terminal) {
        Assert.notNull(terminal, "terminal is null");
        this.terminal = terminal;
    }

    public AnsiConsole cyan(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_CYAN, format, args);
    }

    public AnsiConsole red(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_RED, format, args);
    }

    public AnsiConsole green(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_GREEN, format, args);
    }

    public AnsiConsole blue(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_BLUE, format, args);
    }

    public AnsiConsole yellow(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_YELLOW, format, args);
    }

    public AnsiConsole magenta(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_MAGENTA, format, args);
    }

    private AnsiConsole printf(AnsiColor color, String format, Object... args) {
        return print(color, String.format(Locale.US, format, args));
    }

    public AnsiConsole print(AnsiColor color, String text) {
        try {
            lock.lock();
            terminal.writer().print(AnsiOutput.toString(color, text, AnsiColor.DEFAULT));
            terminal.writer().flush();
            return this;
        } finally {
            lock.unlock();
        }
    }

    public AnsiConsole nl() {
        try {
            lock.lock();
            terminal.writer().println();
            terminal.writer().flush();
            return this;
        } finally {
            lock.unlock();
        }
    }

    private static final String CUU = "\u001B[A";

    private static final String DL = "\u001B[1M";

    private boolean progressBarEnabled = true;

    public boolean toggleProgressBar() {
        progressBarEnabled = !progressBarEnabled;
        return progressBarEnabled;
    }

    public void progressBar(long current, long total, String label) {
        if (!progressBarEnabled) {
            return;
        }

        double p = (current + 0.0) / (Math.max(1, total) + 0.0);
        int ticks = Math.max(0, (int) (30 * p) - 1);

        String bar = String.format(
                "%,9d/%-,9d %5.1f%% [%-30s] %s",
                current,
                total,
                p * 100.0,
                new String(new char[ticks]).replace('\0', '#') + ">",
                label);

        try {
            lock.lock();
            terminal.writer().println(CUU + "\r" + DL + bar);
            terminal.writer().flush();
        } finally {
            lock.unlock();
        }
    }

    public void progressBar(long current, long total, String label, double requestsPerSec, long remainingMillis) {
        if (!progressBarEnabled) {
            return;
        }

        double p = (current + 0.0) / (Math.max(1, total) + 0.0);
        int ticks = Math.max(0, (int) (30 * p) - 1);

        String bar = String.format(
                "%,9d/%-,9d %5.1f%% [%-30s] %,7.0f/s eta %s (%s)",
                current,
                total,
                p * 100.0,
                new String(new char[ticks]).replace('\0', '#') + ">",
                requestsPerSec,
                DurationUtils.millisecondsToDisplayString(remainingMillis),
                label);

        try {
            lock.lock();
            terminal.writer().println(CUU + "\r" + DL + bar);
            terminal.writer().flush();
        } finally {
            lock.unlock();
        }
    }
}
