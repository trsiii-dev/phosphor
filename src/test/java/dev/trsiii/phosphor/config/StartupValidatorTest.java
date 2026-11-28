package dev.trsiii.phosphor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StartupValidatorTest {

    private static final List<String> PAGES = List.of("LIGHTS", "FLIGHT", "VITALS");

    /** One valid screen. Tests add their own YAML before or after it. */
    private static final String ONE_SCREEN = """
            screens:
              - name: left-mfd
                x: 0
                y: 0
                width: 1024
                height: 768
                page: LIGHTS
            """;

    private static List<String> problemsOf(String yaml) {
        return StartupValidator.problems(ConfigLoader.parse(yaml), PAGES);
    }

    @Test
    void aGoodConfigHasNoProblems() {
        assertThat(problemsOf(ONE_SCREEN)).isEmpty();
    }

    @Test
    void aPageThatDoesNotExistIsNamedWithThePagesThatDo() {
        List<String> problems = problemsOf("""
                screens:
                  - name: left-mfd
                    x: 0
                    y: 0
                    width: 1024
                    height: 768
                    page: LIGHT
                """);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[1].page", "'LIGHT'", "LIGHTS, FLIGHT, VITALS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.0.0.53", "127.255.255.254", "localhost", "LOCALHOST", "::1"})
    void theStreamMayBeThisMachineByAnyOfItsNames(String address) {
        assertThat(StartupValidator.isThisMachine(address)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "192.0.2.10", "203.0.113.5", "example.com", "host.example", "", "0.0.0.0",
                // Loopback written in a form that another parser could read differently.
                "0127.0.0.1", "127.1", "2130706433", "127.0.0.1.", "localhost.", "localhost.example.com",
                " 127.0.0.1", "127.0.0.1 ", "127.0.0.1:2942", "::ffff:127.0.0.1", "[::1]", "::1%1"
            })
    void theStreamMayNotBeAnywhereElse(String address) {
        assertThat(StartupValidator.isThisMachine(address)).isFalse();
    }

    @Test
    void aStreamAddressOnAnotherMachineStopsTheProgram() {
        List<String> problems = problemsOf("""
                stream:
                  address: 192.0.2.10
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("stream.address", "192.0.2.10");
    }

    @Test
    void staleMustComeBeforeGivingUp() {
        List<String> problems = problemsOf("""
                stream:
                  staleAfter: 10s
                  giveUpAfter: 10s
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("stream.staleAfter", "stream.giveUpAfter");
    }

    @Test
    void aTimeOfZeroIsRefused() {
        List<String> problems = problemsOf("""
                stream:
                  reconnectInterval: 0s
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("stream.reconnectInterval");
    }

    @Test
    void aPortMustBeAPort() {
        List<String> problems = problemsOf("""
                stream:
                  port: 70000
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("stream.port", "70000");
    }

    @Test
    void noScreensIsAProblemUnlessWindowed() {
        List<String> problems = problemsOf("units: metric");
        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens");

        assertThat(problemsOf("""
                display:
                  windowed: true
                """)).isEmpty();
    }

    @Test
    void twoScreensCannotShareAName() {
        List<String> problems = problemsOf(ONE_SCREEN + """
                  - name: Left-MFD
                    x: 1024
                    y: 0
                    width: 1024
                    height: 768
                    page: FLIGHT
                """);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[2].name");
    }

    @Test
    void twoEntriesCannotBeTheSameScreen() {
        List<String> problems = problemsOf(ONE_SCREEN + """
                  - name: center-mfd
                    x: 0
                    y: 0
                    width: 1024
                    height: 768
                    page: FLIGHT
                """);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[2]", "same position and size");
    }

    @Test
    void aRotationMustBeAQuarterTurn() {
        List<String> problems = problemsOf("""
                screens:
                  - name: left-mfd
                    x: 0
                    y: 0
                    width: 1024
                    height: 768
                    page: LIGHTS
                    rotation: 45
                """);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[1].rotation", "45");
    }

    @Test
    void everyProblemIsFoundInOnePass() {
        List<String> problems = problemsOf("""
                stream:
                  port: 0
                display:
                  frameRateCap: 500
                  textScale: 9
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(3);
    }

    @Test
    void validateThrowsWithEveryProblemInItsMessage() {
        AppConfig config = ConfigLoader.parse("""
                stream:
                  port: 0
                display:
                  frameRateCap: 500
                """ + ONE_SCREEN);

        assertThatThrownBy(() -> StartupValidator.validate(config, PAGES))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("stream.port")
                .hasMessageContaining("display.frameRateCap");
    }

    @Test
    void aTimeOverAnHourIsRefused() {
        List<String> problems = problemsOf("""
                stream:
                  giveUpAfter: 120m
                """ + ONE_SCREEN);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("stream.giveUpAfter");
    }

    @Test
    void aScreenNameMustBeShort() {
        List<String> problems = problemsOf(ONE_SCREEN.replace("left-mfd", "x".repeat(33)));

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[1].name");
    }

    @Test
    void aScreenNameMayNotHoldControlCharacters() {
        // The YAML escape in double quotes puts a tab in the name.
        List<String> problems = problemsOf(ONE_SCREEN.replace("name: left-mfd", "name: \"left\\tmfd\""));

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[1].name");
    }

    @Test
    void aScreenSizeHasAnUpperLimit() {
        List<String> problems = problemsOf(ONE_SCREEN.replace("width: 1024", "width: 99999"));

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).contains("screens[1].width");
    }
}
