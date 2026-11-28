package dev.trsiii.phosphor.tracker;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.StreamPhase.Closed;
import dev.trsiii.phosphor.tracker.StreamPhase.Gone;
import dev.trsiii.phosphor.tracker.StreamPhase.Live;
import dev.trsiii.phosphor.tracker.StreamPhase.Stale;
import dev.trsiii.phosphor.tracker.StreamPhase.Waiting;
import java.time.Instant;
import java.util.Optional;

/**
 * Everything a page draws from. A snapshot never changes. The tracker publishes
 * a new one instead, so the drawing thread needs no lock.
 *
 * @param stream      state of the telemetry stream
 * @param at          when the snapshot was made
 * @param lastMessage when the newest stream message arrived, empty if none is held
 * @param telemetry   the newest values, or {@link Telemetry#EMPTY} while the stream is absent
 * @param units       the units setting, carried here so a page needs nothing but the snapshot
 */
public record Snapshot(
        StreamState stream, Instant at, Optional<Instant> lastMessage, Telemetry telemetry, UnitSystem units) {

    /** A snapshot with no values, in metric. */
    public static Snapshot of(StreamPhase phase, Instant at) {
        return of(phase, at, Telemetry.EMPTY, UnitSystem.METRIC);
    }

    /**
     * @param newest the values of the newest message. They are kept only while
     *               the stream is live or stale.
     */
    public static Snapshot of(StreamPhase phase, Instant at, Telemetry newest, UnitSystem units) {
        Optional<Instant> lastMessage = switch (phase) {
            case Live live -> Optional.of(live.lastMessage());
            case Stale stale -> Optional.of(stale.lastMessage());
            case Waiting waiting -> Optional.empty();
            case Gone gone -> Optional.empty();
            case Closed closed -> Optional.empty();
        };
        Telemetry held = lastMessage.isPresent() ? newest : Telemetry.EMPTY;
        return new Snapshot(phase.state(), at, lastMessage, held, units);
    }
}
