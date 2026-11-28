package dev.trsiii.phosphor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommandLineTest {

    @Test
    void nothingTypedMeansTheUsualConfigAndNoFlags() {
        CommandLine commandLine = CommandLine.parse();

        assertThat(commandLine.config()).isEmpty();
        assertThat(commandLine.help()).isFalse();
        assertThat(commandLine.version()).isFalse();
    }

    @Test
    void theConfigFileCanFollowTheOptionOrBeJoinedToIt() {
        assertThat(CommandLine.parse("--config", "my.yaml").config()).contains(Path.of("my.yaml"));
        assertThat(CommandLine.parse("--config=my.yaml").config()).contains(Path.of("my.yaml"));
    }

    @Test
    void helpAndVersionAreRecognized() {
        assertThat(CommandLine.parse("--help").help()).isTrue();
        assertThat(CommandLine.parse("--version").version()).isTrue();
    }

    @Test
    void configWithoutAFileNameIsRefused() {
        assertThatThrownBy(() -> CommandLine.parse("--config"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--config");
    }

    @Test
    void anUnknownOptionIsNamed() {
        assertThatThrownBy(() -> CommandLine.parse("--fullscreen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--fullscreen");
    }
}
