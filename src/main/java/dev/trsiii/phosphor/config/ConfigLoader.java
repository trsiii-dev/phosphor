package dev.trsiii.phosphor.config;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Loads the config file into an {@link AppConfig}.
 *
 * <p>Jackson only parses the YAML into maps and lists. The fields are then read
 * by hand so that errors can name the exact key and the expected type. Defaults
 * for optional keys are set here.
 *
 * <p>Types are checked here. Ranges and cross-field rules are checked by
 * {@link StartupValidator}.
 */
public final class ConfigLoader {

    private static final YAMLMapper YAML = new YAMLMapper();

    // A real config is a few kB. The cap keeps a huge file from being read into memory.
    private static final long MAX_BYTES = 1_000_000;

    private ConfigLoader() {}

    public static AppConfig load(Path file) {
        String text;
        try {
            if (Files.size(file) > MAX_BYTES) {
                throw new ConfigException("The file is larger than 1 MB, which is too large for a config file.");
            }
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new ConfigException("The file is not UTF-8 text. Save it again as UTF-8.");
        } catch (IOException e) {
            throw new ConfigException("The file could not be read: " + e.getMessage());
        }
        // Strip a UTF-8 byte-order mark if an editor added one.
        if (!text.isEmpty() && text.charAt(0) == 0xFEFF) {
            text = text.substring(1);
        }
        return parse(text);
    }

    public static AppConfig parse(String yaml) {
        Section root = Section.root(read(yaml));

        UnitSystem units = UnitSystem.fromConfig(root.string("units", "metric"));

        Section s = root.section("stream");
        StreamConfig stream = new StreamConfig(
                s.bool("enabled", true),
                s.string("address", "127.0.0.1"),
                s.integer("port", 2942),
                s.duration("connectTimeout", Duration.ofSeconds(1)),
                s.duration("reconnectInterval", Duration.ofSeconds(5)),
                s.duration("staleAfter", Duration.ofSeconds(2)),
                s.duration("giveUpAfter", Duration.ofSeconds(10)));
        s.rejectUnknown();

        Section d = root.section("display");
        DisplayConfig display = new DisplayConfig(
                d.integer("frameRateCap", 20),
                d.bool("windowed", false),
                d.decimal("textScale", 1.0));
        d.rejectUnknown();

        List<ScreenConfig> screens = new ArrayList<>();
        for (Section screen : root.sections("screens")) {
            screens.add(new ScreenConfig(
                    screen.requiredString("name"),
                    screen.requiredInteger("x"),
                    screen.requiredInteger("y"),
                    screen.requiredInteger("width"),
                    screen.requiredInteger("height"),
                    screen.requiredString("page").toUpperCase(Locale.ROOT),
                    screen.integer("rotation", 0)));
            screen.rejectUnknown();
        }

        root.rejectUnknown();
        return new AppConfig(units, stream, display, screens);
    }

    /** Parses the YAML. A file with only blank lines and comments gives an empty map. */
    private static Object read(String yaml) {
        boolean nothingWritten = yaml.lines().map(String::strip).allMatch(line -> line.isEmpty() || line.startsWith("#"));
        if (nothingWritten) {
            return Map.of();
        }
        try {
            return YAML.readValue(yaml, Object.class);
        } catch (RuntimeException e) {
            // Jackson's parse errors are unchecked. The message includes the line number.
            throw new ConfigException("The file is not valid YAML. " + e.getMessage());
        } catch (StackOverflowError e) {
            // Extreme nesting can exhaust the stack before the parser's own limit applies.
            throw new ConfigException("The file is not valid YAML. It is nested too deeply.");
        }
    }
}
