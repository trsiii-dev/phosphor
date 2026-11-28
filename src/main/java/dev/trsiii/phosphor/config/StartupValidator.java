package dev.trsiii.phosphor.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates the config at startup, both single values and cross-field rules. All
 * problems are reported together, and each message starts with the key.
 *
 * <p>TODO: check that every configured screen is connected. This needs the display layer.
 */
public final class StartupValidator {

    private static final Set<Integer> ROTATIONS = Set.of(0, 90, 180, 270);

    // Upper bounds, so a bad config cannot ask for absurd timers, windows or log lines.
    private static final int MAX_SCREENS = 16;
    private static final int MAX_SIDE = 16_384;
    private static final int MAX_OFFSET = 100_000;
    private static final int MAX_NAME = 32;
    private static final int MAX_QUOTED = 60;

    // 127.x.y.z in plain dotted decimal. Short forms and leading zeros are rejected
    // because other parsers read them differently.
    private static final Pattern LOOPBACK_V4 = Pattern.compile("127(\\.(0|[1-9][0-9]{0,2})){3}");

    private StartupValidator() {}

    /** Throws a single {@link ConfigException} with one line per problem. */
    public static void validate(AppConfig config, Collection<String> knownPages) {
        List<String> problems = problems(config, knownPages);
        if (!problems.isEmpty()) {
            throw new ConfigException(String.join(System.lineSeparator(), problems));
        }
    }

    /** Returns one message per problem, or an empty list. */
    public static List<String> problems(AppConfig config, Collection<String> knownPages) {
        List<String> found = new ArrayList<>();

        StreamConfig stream = config.stream();
        if (!isThisMachine(stream.address())) {
            found.add("stream.address: " + quote(stream.address()) + " is not this computer. Phosphor reads the"
                    + " stream only from the machine it runs on: use 127.0.0.1.");
        }
        if (stream.port() < 1 || stream.port() > 65535) {
            found.add("stream.port: " + stream.port() + " is not a port. Use a number from 1 to 65535;"
                    + " the game uses 2942.");
        }
        inRange(found, "stream.connectTimeout", stream.connectTimeout(), Duration.ofMillis(100), Duration.ofMinutes(1));
        inRange(found, "stream.reconnectInterval", stream.reconnectInterval(), Duration.ofMillis(250), Duration.ofHours(1));
        inRange(found, "stream.staleAfter", stream.staleAfter(), Duration.ofMillis(100), Duration.ofHours(1));
        inRange(found, "stream.giveUpAfter", stream.giveUpAfter(), Duration.ofMillis(100), Duration.ofHours(1));
        if (stream.staleAfter().compareTo(stream.giveUpAfter()) >= 0) {
            found.add("stream.staleAfter: " + Durations.show(stream.staleAfter()) + " must be shorter than"
                    + " stream.giveUpAfter, which is " + Durations.show(stream.giveUpAfter()) + ".");
        }

        DisplayConfig display = config.display();
        if (display.frameRateCap() < 1 || display.frameRateCap() > 60) {
            found.add("display.frameRateCap: " + display.frameRateCap() + " is out of range. Use 1 to 60.");
        }
        // Negated form so that NaN fails the check too.
        if (!(display.textScale() >= 0.5 && display.textScale() <= 3.0)) {
            found.add("display.textScale: " + display.textScale() + " is out of range. Use 0.5 to 3.0.");
        }

        List<ScreenConfig> screens = config.screens();
        if (screens.isEmpty() && !display.windowed()) {
            found.add("screens: list at least one screen, or set display.windowed to true.");
        }
        if (screens.size() > MAX_SCREENS) {
            found.add("screens: " + screens.size() + " screens is too many. The limit is " + MAX_SCREENS + ".");
        }
        Set<String> names = new HashSet<>();
        Set<List<Integer>> places = new HashSet<>();
        for (int i = 0; i < screens.size(); i++) {
            ScreenConfig screen = screens.get(i);
            String at = "screens[" + (i + 1) + "]";
            if (screen.width() < 1 || screen.width() > MAX_SIDE) {
                found.add(at + ".width: " + screen.width() + " is out of range. Use 1 to " + MAX_SIDE + ".");
            }
            if (screen.height() < 1 || screen.height() > MAX_SIDE) {
                found.add(at + ".height: " + screen.height() + " is out of range. Use 1 to " + MAX_SIDE + ".");
            }
            if (screen.x() < -MAX_OFFSET || screen.x() > MAX_OFFSET) {
                found.add(at + ".x: " + screen.x() + " is out of range. Use -" + MAX_OFFSET + " to " + MAX_OFFSET + ".");
            }
            if (screen.y() < -MAX_OFFSET || screen.y() > MAX_OFFSET) {
                found.add(at + ".y: " + screen.y() + " is out of range. Use -" + MAX_OFFSET + " to " + MAX_OFFSET + ".");
            }
            if (!ROTATIONS.contains(screen.rotation())) {
                found.add(at + ".rotation: " + screen.rotation() + " is not a rotation. Use 0, 90, 180, or 270.");
            }
            if (!knownPages.contains(screen.page())) {
                found.add(at + ".page: " + quote(screen.page()) + " is not a page. The pages are: "
                        + String.join(", ", knownPages) + ".");
            }
            // The name ends up in the log, so it has to be short, printable text.
            if (screen.name().length() > MAX_NAME || !isPlainText(screen.name())) {
                found.add(at + ".name: use at most " + MAX_NAME + " characters and no control characters.");
            } else if (!names.add(screen.name().toLowerCase(Locale.ROOT))) {
                found.add(at + ".name: '" + screen.name() + "' is used by another screen. Give each its own name.");
            }
            if (!places.add(List.of(screen.x(), screen.y(), screen.width(), screen.height()))) {
                found.add(at + ": another entry has the same position and size. List each screen once.");
            }
        }
        return found;
    }

    static boolean isThisMachine(String address) {
        return loopbackAddress(address).isPresent();
    }

    /**
     * The loopback address for "localhost", a dotted 127.x.y.z address, or ::1, and
     * empty for anything else. No DNS lookup is done.
     *
     * <p>The stream client must connect to the address returned here. It must not
     * resolve the config text a second time, because a resolver could read the same
     * text differently.
     */
    static Optional<InetAddress> loopbackAddress(String address) {
        try {
            if (address.toLowerCase(Locale.ROOT).equals("localhost")) {
                return Optional.of(InetAddress.getByAddress("localhost", new byte[] {127, 0, 0, 1}));
            }
            if (LOOPBACK_V4.matcher(address).matches() || address.equals("::1")) {
                InetAddress parsed = InetAddress.ofLiteral(address);
                if (parsed.isLoopbackAddress()) {
                    return Optional.of(parsed);
                }
            }
        } catch (IllegalArgumentException | UnknownHostException e) {
            // An octet over 255, for example.
        }
        return Optional.empty();
    }

    private static void inRange(List<String> found, String entry, Duration value, Duration min, Duration max) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            found.add(entry + ": " + Durations.show(value) + " is out of range. Use " + Durations.show(min) + " to "
                    + Durations.show(max) + ".");
        }
    }

    private static boolean isPlainText(String text) {
        return text.codePoints()
                .noneMatch(cp -> Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT);
    }

    // Config text quoted in a message, cut short so a bad value cannot flood the log.
    private static String quote(String text) {
        return "'" + (text.length() > MAX_QUOTED ? text.substring(0, MAX_QUOTED) + "..." : text) + "'";
    }
}
