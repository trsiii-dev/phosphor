package dev.trsiii.phosphor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** OS name, environment and home folder are passed in, so both platforms are tested on either one. */
class ConfigLocationTest {

    private static final Path HOME = Path.of("home", "pilot");

    @Test
    void onWindowsTheConfigIsUnderAppData() {
        // Absolute on whichever OS runs the test.
        Path roaming = Path.of("roaming").toAbsolutePath();
        Path local = Path.of("local").toAbsolutePath();
        Map<String, String> env = Map.of("APPDATA", roaming.toString(), "LOCALAPPDATA", local.toString());

        assertThat(ConfigLocation.configDir("Windows 11", env::get, HOME)).isEqualTo(roaming.resolve("phosphor"));
        assertThat(ConfigLocation.logDir("Windows 11", env::get, HOME))
                .isEqualTo(local.resolve("phosphor").resolve("logs"));
    }

    @Test
    void onWindowsARelativeVariableIsIgnored() {
        Map<String, String> env = Map.of("APPDATA", "roaming", "LOCALAPPDATA", "local");

        assertThat(ConfigLocation.configDir("Windows 11", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of("AppData", "Roaming", "phosphor")));
        assertThat(ConfigLocation.logDir("Windows 11", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of("AppData", "Local", "phosphor", "logs")));
    }

    @Test
    void onWindowsWithoutTheVariablesTheUsualFoldersAreUsed() {
        Map<String, String> env = Map.of();

        assertThat(ConfigLocation.configDir("Windows 11", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of("AppData", "Roaming", "phosphor")));
        assertThat(ConfigLocation.logDir("Windows 11", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of("AppData", "Local", "phosphor", "logs")));
    }

    @Test
    void onLinuxTheConfigIsUnderDotConfig() {
        Map<String, String> env = Map.of();

        assertThat(ConfigLocation.configDir("Linux", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of(".config", "phosphor")));
        assertThat(ConfigLocation.logDir("Linux", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of(".local", "state", "phosphor")));
    }

    @Test
    void onLinuxTheXdgVariablesMoveTheFolders() {
        // Absolute on whichever OS runs the test.
        Path elsewhere = Path.of("elsewhere").toAbsolutePath();
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", elsewhere.toString(), "XDG_STATE_HOME", elsewhere.toString());

        assertThat(ConfigLocation.configDir("Linux", env::get, HOME)).isEqualTo(elsewhere.resolve("phosphor"));
        assertThat(ConfigLocation.logDir("Linux", env::get, HOME)).isEqualTo(elsewhere.resolve("phosphor"));
    }

    @Test
    void onLinuxAnXdgVariableThatIsNotAFullPathIsIgnored() {
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "not/from/the/root");

        assertThat(ConfigLocation.configDir("Linux", env::get, HOME))
                .isEqualTo(HOME.resolve(Path.of(".config", "phosphor")));
    }
}
