package dev.trsiii.phosphor.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.tracker.StreamPhase.Gone;
import dev.trsiii.phosphor.tracker.StreamPhase.Live;
import dev.trsiii.phosphor.tracker.StreamPhase.Stale;
import dev.trsiii.phosphor.tracker.StreamPhase.Waiting;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SnapshotTest {

    private static final Instant MESSAGE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = MESSAGE.plusSeconds(3);

    @Test
    void aLiveOrStalePhaseKeepsTheTimeOfTheNewestMessage() {
        assertThat(Snapshot.of(new Live(MESSAGE), NOW))
                .isEqualTo(new Snapshot(StreamState.LIVE, NOW, Optional.of(MESSAGE)));
        assertThat(Snapshot.of(new Stale(MESSAGE), NOW).lastMessage()).contains(MESSAGE);
        assertThat(Snapshot.of(new Stale(MESSAGE), NOW).stream()).isEqualTo(StreamState.STALE);
    }

    @Test
    void anAbsentStreamHoldsNoMessageTime() {
        assertThat(Snapshot.of(new Waiting(), NOW).lastMessage()).isEmpty();
        assertThat(Snapshot.of(new Gone(MESSAGE), NOW).lastMessage()).isEmpty();
        assertThat(Snapshot.of(new Gone(MESSAGE), NOW).stream()).isEqualTo(StreamState.ABSENT);
    }
}
