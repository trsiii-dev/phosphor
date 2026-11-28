package dev.trsiii.phosphor.config;

import java.util.List;

/**
 * The parsed config file. Built once at startup by {@link ConfigLoader} and
 * validated by {@link StartupValidator}.
 */
public record AppConfig(UnitSystem units, StreamConfig stream, DisplayConfig display, List<ScreenConfig> screens) {

    public AppConfig {
        // Defensive copy.
        screens = List.copyOf(screens);
    }
}
