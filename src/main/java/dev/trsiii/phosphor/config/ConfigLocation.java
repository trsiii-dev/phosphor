package dev.trsiii.phosphor.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

/**
 * Per-user config and log locations. Keep all OS-specific path logic in this class.
 *
 * <pre>
 *            config file                          log files
 * Windows    %APPDATA%\phosphor\config.yaml       %LOCALAPPDATA%\phosphor\logs
 * Linux      ~/.config/phosphor/config.yaml       ~/.local/state/phosphor
 * </pre>
 *
 * <p>On Linux, XDG_CONFIG_HOME and XDG_STATE_HOME override the two directories.
 */
public final class ConfigLocation {

    private static final String FOLDER = "phosphor";

    private ConfigLocation() {}

    /** Default config file, used when --config is not given. */
    public static Path configFile() {
        return configDir(System.getProperty("os.name"), System::getenv, home()).resolve("config.yaml");
    }

    public static Path logDir() {
        return logDir(System.getProperty("os.name"), System::getenv, home());
    }

    // These overloads take their inputs as parameters so tests can simulate either OS.

    static Path configDir(String osName, Function<String, String> env, Path home) {
        if (isWindows(osName)) {
            return absolute(env.apply("APPDATA"), home.resolve("AppData").resolve("Roaming")).resolve(FOLDER);
        }
        return absolute(env.apply("XDG_CONFIG_HOME"), home.resolve(".config")).resolve(FOLDER);
    }

    static Path logDir(String osName, Function<String, String> env, Path home) {
        if (isWindows(osName)) {
            return absolute(env.apply("LOCALAPPDATA"), home.resolve("AppData").resolve("Local"))
                    .resolve(FOLDER)
                    .resolve("logs");
        }
        return absolute(env.apply("XDG_STATE_HOME"), home.resolve(".local").resolve("state")).resolve(FOLDER);
    }

    private static boolean isWindows(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static Path home() {
        return Path.of(System.getProperty("user.home"));
    }

    // Path from an environment variable, or the fallback if it is unset or invalid.
    private static Path named(String value, Path fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            return fallback;
        }
    }

    // Like named(), but a relative path is ignored. The XDG spec requires that, and it
    // keeps a bad variable from sending files to the current directory.
    private static Path absolute(String value, Path fallback) {
        Path path = named(value, fallback);
        return path.isAbsolute() ? path : fallback;
    }
}
