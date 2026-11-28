package dev.trsiii.phosphor.config;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats config durations such as {@code 250ms}, {@code 2s}, and {@code 1m}. */
final class Durations {

    private static final Pattern FORM = Pattern.compile("(\\d{1,9})\\s*(ms|s|m)");

    private Durations() {}

    /** Returns empty if the text is not a valid duration. */
    static Optional<Duration> parse(String text) {
        Matcher match = FORM.matcher(text.strip().toLowerCase(Locale.ROOT));
        if (!match.matches()) {
            return Optional.empty();
        }
        long amount = Long.parseLong(match.group(1));
        return Optional.of(switch (match.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            default -> Duration.ofMinutes(amount);
        });
    }

    /** Formats a duration for error messages. */
    static String show(Duration duration) {
        long millis = duration.toMillis();
        if (millis != 0 && millis % 60_000 == 0) {
            return (millis / 60_000) + "m";
        }
        return millis % 1000 == 0 ? (millis / 1000) + "s" : millis + "ms";
    }
}
