package dev.trsiii.phosphor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    private static final List<String> PAGES = List.of("LIGHTS", "FLIGHT", "VITALS");

    @Test
    void anEmptyFileGivesEveryDefault() {
        AppConfig config = ConfigLoader.parse("");

        assertThat(config.units()).isEqualTo(UnitSystem.METRIC);
        assertThat(config.stream().enabled()).isTrue();
        assertThat(config.stream().address()).isEqualTo("127.0.0.1");
        assertThat(config.stream().port()).isEqualTo(2942);
        assertThat(config.stream().staleAfter()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.stream().giveUpAfter()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.display().frameRateCap()).isEqualTo(20);
        assertThat(config.display().windowed()).isFalse();
        assertThat(config.screens()).isEmpty();
    }

    @Test
    void aFileOfNothingButNotesIsEmptyToo() {
        AppConfig config = ConfigLoader.parse("""
                # nothing here

                # yet
                """);

        assertThat(config.units()).isEqualTo(UnitSystem.METRIC);
    }

    @Test
    void everyEntryIsRead() {
        AppConfig config = ConfigLoader.parse("""
                units: imperial
                stream:
                  enabled: false
                  address: localhost
                  port: 50051
                  connectTimeout: 500ms
                  reconnectInterval: 3s
                  staleAfter: 4s
                  giveUpAfter: 1m
                display:
                  frameRateCap: 30
                  windowed: true
                  textScale: 1.5
                screens:
                  - name: left-mfd
                    x: -1024
                    y: 0
                    width: 1024
                    height: 768
                    page: lights
                    rotation: 90
                """);

        assertThat(config.units()).isEqualTo(UnitSystem.IMPERIAL);
        assertThat(config.stream())
                .isEqualTo(new StreamConfig(
                        false,
                        "localhost",
                        50051,
                        Duration.ofMillis(500),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(4),
                        Duration.ofMinutes(1)));
        assertThat(config.display()).isEqualTo(new DisplayConfig(30, true, 1.5));
        // Page names are stored in upper case.
        assertThat(config.screens()).containsExactly(new ScreenConfig("left-mfd", -1024, 0, 1024, 768, "LIGHTS", 90));
    }

    @Test
    void theExampleFileLoadsAndPassesEveryCheck() {
        // Gradle runs tests from the project folder, so this is the committed example.
        AppConfig config = ConfigLoader.load(Path.of("run", "config.example.yaml"));

        assertThat(config.screens()).hasSize(3);
        assertThat(StartupValidator.problems(config, PAGES)).isEmpty();
    }

    @Test
    void aWrongUnitsWordNamesTheThreeThatAreAllowed() {
        assertThatThrownBy(() -> ConfigLoader.parse("units: furlongs"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("units")
                .hasMessageContaining("furlongs")
                .hasMessageContaining("metric, imperial, or aviation");
    }

    @Test
    void aMistypedEntryIsPointedOut() {
        assertThatThrownBy(() -> ConfigLoader.parse("""
                        stream:
                          prot: 2942
                        """))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("stream.prot");
    }

    @Test
    void aWordWhereANumberBelongsSaysWhatWasExpected() {
        assertThatThrownBy(() -> ConfigLoader.parse("""
                        stream:
                          port: abc
                        """))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("stream.port")
                .hasMessageContaining("a whole number")
                .hasMessageContaining("abc");
    }

    @Test
    void aTimeWithoutAUnitIsExplained() {
        assertThatThrownBy(() -> ConfigLoader.parse("""
                        stream:
                          staleAfter: 2
                        """))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("stream.staleAfter")
                .hasMessageContaining("2s");
    }

    @Test
    void aScreenWithoutAPageIsNamedByItsPlaceInTheList() {
        assertThatThrownBy(() -> ConfigLoader.parse("""
                        screens:
                          - name: left-mfd
                            x: 0
                            y: 0
                            width: 1024
                            height: 768
                        """))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("screens[1].page");
    }

    @Test
    void aFileThatIsNotYamlIsReportedAsSuch() {
        assertThatThrownBy(() -> ConfigLoader.parse("stream: [unclosed"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("not valid YAML");
    }

    @Test
    void aFileThatIsOneWordIsNotAConfig() {
        assertThatThrownBy(() -> ConfigLoader.parse("hello"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("expected a group of entries");
    }

    @Test
    void deeplyNestedYamlIsRejected() {
        String yaml = "[".repeat(10_000) + "]".repeat(10_000);

        assertThatThrownBy(() -> ConfigLoader.parse(yaml)).isInstanceOf(ConfigException.class);
    }

    @Test
    void aFileOverOneMegabyteIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("big.yaml");
        Files.writeString(file, "#".repeat(1_100_000));

        assertThatThrownBy(() -> ConfigLoader.load(file))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("1 MB");
    }
}
