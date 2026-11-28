package dev.trsiii.phosphor.tracker;

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
 * <p>TODO: add the ship and pilot values with the stream client.
 *
 * @param stream      state of the telemetry stream
 * @param at          when the snapshot was made
 * @param lastMessage when the newest stream message arrived, empty if none is held
 */
public record Snapshot(StreamState stream, Instant at, Optional<Instant> lastMessage) {

    public static Snapshot of(StreamPhase phase, Instant at) {
        Optional<Instant> lastMessage = switch (phase) {
            case Live live -> Optional.of(live.lastMessage());
            case Stale stale -> Optional.of(stale.lastMessage());
            case Waiting waiting -> Optional.empty();
            case Gone gone -> Optional.empty();
            case Closed closed -> Optional.empty();
        };
        return new Snapshot(phase.state(), at, lastMessage);
    }
}
