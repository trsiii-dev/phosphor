package dev.trsiii.phosphor.tracker;

import dev.trsiii.phosphor.tracker.StreamPhase.Live;
import dev.trsiii.phosphor.tracker.StreamPhase.Stale;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamEnded;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamMessage;
import dev.trsiii.phosphor.tracker.TrackerEvent.Tick;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The single writer of the program's state. Sources and timers post events from
 * their own threads, one thread here takes them in order, and the pages read the
 * newest snapshot. Nothing else is shared, so nothing needs a lock.
 */
public final class Tracker {

    private static final System.Logger LOG = System.getLogger(Tracker.class.getName());

    // At most ten snapshots a second, however fast messages arrive.
    private static final Duration PUBLISH_INTERVAL = Duration.ofMillis(100);

    // Bounded, so a source that floods the tracker cannot use up memory.
    private static final int QUEUE_SIZE = 1024;

    private final StreamMachine machine;
    private final Runnable onGameClosed;
    private final BlockingQueue<TrackerEvent> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final AtomicReference<Snapshot> snapshot;
    private final AtomicBoolean dropping = new AtomicBoolean();
    private final Thread thread = new Thread(this::run, "phosphor-tracker");

    // Touched only by the tracker thread.
    private StreamPhase phase;
    private Instant lastPublished;

    /**
     * @param now          start time, from the same clock that stamps the events
     * @param onGameClosed run once, on the tracker thread, when the game has closed
     */
    public Tracker(StreamMachine machine, Instant now, Runnable onGameClosed) {
        this.machine = machine;
        this.onGameClosed = onGameClosed;
        this.phase = machine.start();
        this.snapshot = new AtomicReference<>(Snapshot.of(phase, now));
        this.lastPublished = now;
        // A daemon thread never keeps the program alive on its own.
        thread.setDaemon(true);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    /** The newest snapshot. Safe to call from any thread. */
    public Snapshot snapshot() {
        return snapshot.get();
    }

    /** Queues an event. Safe to call from any thread, and never blocks: a full queue drops the event. */
    public void post(TrackerEvent event) {
        if (!queue.offer(event) && dropping.compareAndSet(false, true)) {
            LOG.log(System.Logger.Level.WARNING, "The tracker is behind. Events are being dropped.");
        }
    }

    private void run() {
        try {
            while (true) {
                handle(queue.take());
            }
        } catch (InterruptedException e) {
            // stop() was called.
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "The tracker stopped on an error.", e);
            // Never leave the pages showing live values that no longer update.
            snapshot.set(new Snapshot(StreamState.ABSENT, snapshot.get().at(), Optional.empty()));
        }
    }

    /** Applies one event. Runs on the tracker thread. Tests call it directly. */
    void handle(TrackerEvent event) {
        StreamPhase before = phase;
        phase = machine.next(before, event);

        boolean changed = phase.getClass() != before.getClass();
        if (changed) {
            LOG.log(System.Logger.Level.INFO, "Stream " + name(before) + " -> " + name(phase) + ": " + cause(event));
        }

        // While live or stale the snapshot's time matters, because pages show how old the values are.
        boolean aging = phase instanceof Live || phase instanceof Stale;
        if (changed || (aging && !event.at().isBefore(lastPublished.plus(PUBLISH_INTERVAL)))) {
            snapshot.set(Snapshot.of(phase, event.at()));
            lastPublished = event.at();
        }

        // Closed is final, so this runs once.
        if (changed && phase.gameHasClosed()) {
            onGameClosed.run();
        }
    }

    private static String name(StreamPhase phase) {
        return phase.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    private static String cause(TrackerEvent event) {
        return switch (event) {
            case StreamMessage message -> "a message arrived";
            case StreamEnded ended -> ended.cause();
            case Tick tick -> "no message in time";
        };
    }
}
