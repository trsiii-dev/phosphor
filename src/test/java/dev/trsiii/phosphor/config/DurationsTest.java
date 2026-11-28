package dev.trsiii.phosphor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DurationsTest {

    @ParameterizedTest
    @CsvSource({"250ms, 250", "2s, 2000", "1m, 60000", "2 s, 2000", "10S, 10000", "0s, 0"})
    void aNumberAndAUnitIsALengthOfTime(String text, long millis) {
        assertThat(Durations.parse(text)).contains(Duration.ofMillis(millis));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "s", "two seconds", "2h", "-2s", "1.5s", ""})
    void anythingElseIsNot(String text) {
        assertThat(Durations.parse(text)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"250, 250ms", "2000, 2s", "90000, 90s", "60000, 1m", "3600000, 60m", "1500, 1500ms", "0, 0s"})
    void aLengthOfTimeIsShownInTheSameForm(long millis, String shown) {
        assertThat(Durations.show(Duration.ofMillis(millis))).isEqualTo(shown);
    }
}
