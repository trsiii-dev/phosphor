package dev.trsiii.phosphor.tracker;

import dev.trsiii.phosphor.telemetry.Telemetry;
import java.time.Instant;

/**
 * Input event for the tracker. Sources and timers put these on the tracker's
 * queue, and the tracker handles them one at a time.
 *
 * <p>TODO: add two more events, for ship data arriving and for it failing to arrive.
 */
public sealed interface TrackerEvent {

    /** When it happened. The machine takes the time from here and never reads a clock. */
    Instant at();

    /**
     * A message arrived from the stream.
     *
     * @param at        arrival time
     * @param telemetry the values it carried
     */
    record StreamMessage(Instant at, Telemetry telemetry) implements TrackerEvent {

        /** A message that carried no values. */
        public StreamMessage(Instant at) {
            this(at, Telemetry.EMPTY);
        }
    }

    /**
     * The stream closed, failed, or could not be reached.
     *
     * @param at    when it was noticed
     * @param cause short text for the log, such as "the game closed the stream"
     */
    record StreamEnded(Instant at, String cause) implements TrackerEvent {}

    /**
     * The staleness timer fired. Ticks are how the machine notices that the
     * newest message has become too old.
     *
     * @param at when the timer fired
     */
    record Tick(Instant at) implements TrackerEvent {}
}
