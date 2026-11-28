package dev.trsiii.phosphor;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.config.AppConfig;
import dev.trsiii.phosphor.config.ConfigLoader;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void theStartupLineSaysWhatTheConfigAskedFor() {
        AppConfig config = ConfigLoader.parse("""
                units: aviation
                screens:
                  - name: left-mfd
                    x: 0
                    y: 0
                    width: 1024
                    height: 768
                    page: lights
                """);

        assertThat(Main.startupLine("1.1", config))
                .contains("Phosphor 1.1", "units aviation", "stream 127.0.0.1:2942", "left-mfd LIGHTS");
    }

    @Test
    void aStreamThatIsSwitchedOffIsSaidToBeOff() {
        AppConfig config = ConfigLoader.parse("""
                stream:
                  enabled: false
                """);

        assertThat(Main.startupLine("1.1", config)).contains("stream off", "no screens");
    }

    @Test
    void theBuildNumberComesFromTheBuild() {
        // Gradle writes the version into build.properties before the tests run.
        assertThat(BuildInfo.version()).isEqualTo("1.1");
    }
}
