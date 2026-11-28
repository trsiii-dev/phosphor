package dev.trsiii.phosphor.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One YAML mapping from the config file, such as the {@code stream} block. It
 * tracks its path so that errors can give the full key ({@code stream.port}), and
 * it records which keys were read so that unknown keys can be rejected.
 */
final class Section {

    private final String path;
    private final Map<String, Object> values;
    private final Set<String> asked = new LinkedHashSet<>();

    private Section(String path, Map<String, Object> values) {
        this.path = path;
        this.values = values;
    }

    static Section root(Object raw) {
        return of("", raw);
    }

    private static Section of(String path, Object raw) {
        if (raw == null) {
            // A missing or empty section gets all defaults.
            return new Section(path, Map.of());
        }
        if (!(raw instanceof Map<?, ?> map)) {
            String where = path.isEmpty() ? "The config file" : path;
            throw new ConfigException(where + ": expected a group of entries, found " + show(raw) + ".");
        }
        Map<String, Object> entries = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            entries.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return new Section(path, entries);
    }

    String string(String key, String fallback) {
        Object value = take(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        throw wrong(key, "a word", value);
    }

    String requiredString(String key) {
        String value = string(key, null);
        if (value == null || value.isBlank()) {
            throw missing(key);
        }
        return value;
    }

    int integer(String key, int fallback) {
        Object value = take(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer number) {
            return number;
        }
        throw wrong(key, "a whole number", value);
    }

    int requiredInteger(String key) {
        if (values.get(key) == null) {
            asked.add(key);
            throw missing(key);
        }
        return integer(key, 0);
    }

    boolean bool(String key, boolean fallback) {
        Object value = take(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        throw wrong(key, "true or false", value);
    }

    double decimal(String key, double fallback) {
        Object value = take(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw wrong(key, "a number", value);
    }

    Duration duration(String key, Duration fallback) {
        Object value = take(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String text) {
            Optional<Duration> parsed = Durations.parse(text);
            if (parsed.isPresent()) {
                return parsed.get();
            }
        }
        throw new ConfigException(at(key) + ": " + show(value)
                + " is not a length of time. Write a number and a unit, as in 2s or 250ms.");
    }

    /** Nested section, such as {@code stream}. A missing key gives an empty section. */
    Section section(String key) {
        return of(at(key), take(key));
    }

    /** List of sections, such as {@code screens}. Error messages use 1-based indexes. */
    List<Section> sections(String key) {
        Object value = take(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> items)) {
            throw wrong(key, "a list, each entry starting with a dash", value);
        }
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String itemPath = at(key) + "[" + (i + 1) + "]";
            if (items.get(i) == null) {
                throw new ConfigException(itemPath + ": the entry is empty.");
            }
            sections.add(of(itemPath, items.get(i)));
        }
        return sections;
    }

    /** Throws on any key that was never read. Call it after reading every expected key. */
    void rejectUnknown() {
        for (String key : values.keySet()) {
            if (!asked.contains(key)) {
                throw new ConfigException(at(key) + ": not an entry Phosphor knows. The entries here are: "
                        + String.join(", ", asked) + ".");
            }
        }
    }

    private Object take(String key) {
        asked.add(key);
        return values.get(key);
    }

    private String at(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private ConfigException missing(String key) {
        return new ConfigException(at(key) + ": this entry is needed.");
    }

    private ConfigException wrong(String key, String expected, Object found) {
        return new ConfigException(at(key) + ": expected " + expected + ", found " + show(found) + ".");
    }

    private static String show(Object value) {
        if (value instanceof Map) {
            return "a group of entries";
        }
        if (value instanceof List) {
            return "a list";
        }
        // Cut short so a bad value cannot flood the log.
        String text = String.valueOf(value);
        return "'" + (text.length() > 60 ? text.substring(0, 60) + "..." : text) + "'";
    }
}
