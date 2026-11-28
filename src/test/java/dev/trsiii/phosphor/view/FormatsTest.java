package dev.trsiii.phosphor.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.source.DemoSource;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamPhase;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FormatsTest {

    private static final Instant MESSAGE = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void theDataStatusFollowsTheStream() {
        assertThat(Formats.dataStatus(Snapshot.of(new StreamPhase.Live(MESSAGE), MESSAGE.plusMillis(100))))
                .isEqualTo("LIVE");
        assertThat(Formats.dataStatus(Snapshot.of(new StreamPhase.Stale(MESSAGE), MESSAGE.plusMillis(2_400))))
                .isEqualTo("2 S OLD");
        assertThat(Formats.dataStatus(Snapshot.of(new StreamPhase.Waiting(), MESSAGE))).isEqualTo("NO STREAM");
    }

    @Test
    void onlyLightsWithAWarningAreCounted() {
        // The sample has one minor and two major warnings.
        assertThat(Formats.litCount(DemoSource.sample(0))).isEqualTo(3);
        // A light with no known state is not a warning.
        assertThat(Formats.litCount(Telemetry.EMPTY)).isZero();
    }

    @Test
    void aYesOrNoValueCanBeUnknown() {
        assertThat(Formats.either(Optional.of(true), "DOWN", "UP")).isEqualTo("DOWN");
        assertThat(Formats.either(Optional.of(false), "DOWN", "UP")).isEqualTo("UP");
        assertThat(Formats.either(Optional.empty(), "DOWN", "UP")).isEqualTo("---");
    }
}
