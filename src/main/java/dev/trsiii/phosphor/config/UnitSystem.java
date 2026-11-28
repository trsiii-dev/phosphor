package dev.trsiii.phosphor.config;

import java.util.Locale;

/** Unit system for every measurement shown on a page. */
public enum UnitSystem {
    METRIC,
    IMPERIAL,
    AVIATION;

    /** Name used in the config file, such as {@code metric}. */
    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses the config value. Throws {@link ConfigException} listing the valid values if it is unknown. */
    static UnitSystem fromConfig(String word) {
        String wanted = word.strip().toLowerCase(Locale.ROOT);
        for (UnitSystem system : values()) {
            if (system.configName().equals(wanted)) {
                return system;
            }
        }
        throw new ConfigException(
                "units: '" + word + "' is not a units setting. Use metric, imperial, or aviation.");
    }
}
