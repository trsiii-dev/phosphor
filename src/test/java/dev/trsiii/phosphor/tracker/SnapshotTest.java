package dev.trsiii.phosphor.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.source.DemoSource;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.StreamPhase.Gone;
import dev.trsiii.phosphor.tracker.StreamPhase.Live;
import dev.trsiii.phosphor.tracker.StreamPhase.Stale;
import dev.trsiii.phosphor.tracker.StreamPhase.Waiting;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SnapshotTest {

    private static final Instant MESSAGE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = MESSAGE.plusSeconds(3);
    private static final Telemetry VALUES = DemoSource.sample(0);

    @Test
    void aLiveOrStalePhaseKeepsTheNewestMessageAndItsTime() {
        Snapshot live = Snapshot.of(new Live(MESSAGE), NOW, VALUES, UnitSystem.AVIATION);

        assertThat(live.stream()).isEqualTo(StreamState.LIVE);
        assertThat(live.at()).isEqualTo(NOW);
        assertThat(live.lastMessage()).contains(MESSAGE);
        assertThat(live.telemetry()).isEqualTo(VALUES);
        assertThat(live.units()).isEqualTo(UnitSystem.AVIATION);

        Snapshot stale = Snapshot.of(new Stale(MESSAGE), NOW, VALUES, UnitSystem.METRIC);

        assertThat(stale.stream()).isEqualTo(StreamState.STALE);
        assertThat(stale.lastMessage()).contains(MESSAGE);
        assertThat(stale.telemetry()).isEqualTo(VALUES);
    }

    @Test
    void anAbsentStreamHoldsNoValues() {
        assertThat(Snapshot.of(new Waiting(), NOW, VALUES, UnitSystem.METRIC).lastMessage()).isEmpty();
        assertThat(Snapshot.of(new Waiting(), NOW, VALUES, UnitSystem.METRIC).telemetry()).isEqualTo(Telemetry.EMPTY);

        Snapshot gone = Snapshot.of(new Gone(MESSAGE), NOW, VALUES, UnitSystem.METRIC);

        assertThat(gone.stream()).isEqualTo(StreamState.ABSENT);
        assertThat(gone.lastMessage()).isEmpty();
        assertThat(gone.telemetry()).isEqualTo(Telemetry.EMPTY);
    }
}
