package dev.trsiii.phosphor.source;

import dev.trsiii.phosphor.tracker.TrackerEvent;
import java.util.function.Consumer;

/** Something that produces game events on its own schedule, such as the telemetry stream. */
public interface GameSource {

    /**
     * Starts producing events. The source may call the sink from any thread, and
     * the sink returns quickly.
     */
    void start(Consumer<TrackerEvent> sink);

    void stop();
}
