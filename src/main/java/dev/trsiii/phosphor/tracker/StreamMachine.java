package dev.trsiii.phosphor.tracker;

import dev.trsiii.phosphor.tracker.StreamPhase.Closed;
import dev.trsiii.phosphor.tracker.StreamPhase.Gone;
import dev.trsiii.phosphor.tracker.StreamPhase.Live;
import dev.trsiii.phosphor.tracker.StreamPhase.Stale;
import dev.trsiii.phosphor.tracker.StreamPhase.Waiting;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamEnded;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamMessage;
import dev.trsiii.phosphor.tracker.TrackerEvent.Tick;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * State machine for the telemetry stream. {@link #next} maps a phase and an event
 * to the next phase.
 *
 * <p>The machine is a pure function with no side effects. The time comes from the
 * event, so nothing here reads a clock and each transition can be tested with one
 * table row.
 *
 * <p>TODO: the tracker will hold the current phase and log each change. It ends the
 * program when the phase is Closed.
 *
 * <pre>
 *            a message                  the stream ends, or the newest
 *  Waiting -------------> Live ------------------------------------> Stale
 *                          ^   message is older than staleAfter        |
 *                          |                                           |
 *                          +----------------- a message ---------------+
 *                          |                                           |
 *                          |              the newest message is older  |
 *                          +-- a message -- Gone <---------------------+
 *                                             |       than giveUpAfter
 *                                             |
 *                                             | gone for longer than giveUpAfter
 *                                             v
 *                                           Closed  (the program ends)
 * </pre>
 */
public final class StreamMachine {

    private final Duration staleAfter;
    private final Duration giveUpAfter;

    /**
     * @param staleAfter  stream.staleAfter from the config
     * @param giveUpAfter stream.giveUpAfter from the config, used for both Stale to Gone and Gone to Closed
     */
    public StreamMachine(Duration staleAfter, Duration giveUpAfter) {
        this.staleAfter = Objects.requireNonNull(staleAfter, "staleAfter");
        this.giveUpAfter = Objects.requireNonNull(giveUpAfter, "giveUpAfter");
    }

    /** Initial phase. */
    public StreamPhase start() {
        return new Waiting();
    }

    /** Transition table. The outer switch is the current phase and the inner switch is the event. */
    public StreamPhase next(StreamPhase now, TrackerEvent event) {
        return switch (now) {
            case Waiting waiting -> switch (event) {
                case StreamMessage message -> new Live(message.at());
                // StreamEnded here is a failed connection attempt. Waiting has no timeout.
                case StreamEnded _ -> waiting;
                case Tick _ -> waiting;
            };
            case Live live -> switch (event) {
                case StreamMessage message -> new Live(message.at());
                case StreamEnded _ -> new Stale(live.lastMessage());
                case Tick tick -> olderThan(live.lastMessage(), tick.at(), staleAfter)
                        ? new Stale(live.lastMessage())
                        : live;
            };
            case Stale stale -> switch (event) {
                case StreamMessage message -> new Live(message.at());
                case StreamEnded _ -> stale;
                case Tick tick -> olderThan(stale.lastMessage(), tick.at(), giveUpAfter)
                        ? new Gone(tick.at())
                        : stale;
            };
            case Gone gone -> switch (event) {
                // The game came back in time.
                case StreamMessage message -> new Live(message.at());
                // A failed reconnect does not reset the giveUpAfter timer.
                case StreamEnded _ -> gone;
                case Tick tick -> olderThan(gone.since(), tick.at(), giveUpAfter) ? new Closed() : gone;
            };
            // Terminal. The program is started again with the game.
            case Closed closed -> closed;
        };
    }

    /** True if strictly more than {@code limit} has passed. Exactly {@code limit} does not count. */
    private static boolean olderThan(Instant then, Instant now, Duration limit) {
        return Duration.between(then, now).compareTo(limit) > 0;
    }
}
