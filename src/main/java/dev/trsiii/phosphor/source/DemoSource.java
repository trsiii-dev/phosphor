package dev.trsiii.phosphor.source;

import dev.trsiii.phosphor.tracker.TrackerEvent;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Stand-in for the game, for working on pages without it. It sends messages
 * for eight seconds, goes silent for five, and repeats, so the pages go live,
 * then stale, then live again. The silence is shorter than the give-up time, so
 * the program never quits on it.
 */
public final class DemoSource implements GameSource {

    // About 17 messages a second, like the game.
    private static final long MESSAGE_MILLIS = 60;
    private static final long LIVE_MILLIS = 8_000;
    private static final long SILENT_MILLIS = 5_000;

    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "phosphor-demo");
        thread.setDaemon(true);
        return thread;
    });

    public DemoSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void start(Consumer<TrackerEvent> sink) {
        Instant began = clock.instant();
        executor.scheduleAtFixedRate(
                () -> {
                    Instant now = clock.instant();
                    long intoCycle = Duration.between(began, now).toMillis() % (LIVE_MILLIS + SILENT_MILLIS);
                    if (intoCycle < LIVE_MILLIS) {
                        sink.accept(new StreamMessage(now));
                    }
                },
                0,
                MESSAGE_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }
}
