package dev.trsiii.phosphor.tracker;

import dev.trsiii.phosphor.tracker.TrackerEvent.Tick;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Posts a Tick four times a second. Ticks are how the tracker notices that messages have stopped. */
public final class Ticker {

    private static final long PERIOD_MILLIS = 250;

    private final Clock clock;
    private final Consumer<TrackerEvent> sink;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "phosphor-ticker");
        thread.setDaemon(true);
        return thread;
    });

    public Ticker(Clock clock, Consumer<TrackerEvent> sink) {
        this.clock = clock;
        this.sink = sink;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                () -> sink.accept(new Tick(clock.instant())), PERIOD_MILLIS, PERIOD_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }
}
