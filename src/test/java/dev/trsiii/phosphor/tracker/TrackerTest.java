package dev.trsiii.phosphor.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.tracker.TrackerEvent.StreamEnded;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamMessage;
import dev.trsiii.phosphor.tracker.TrackerEvent.Tick;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Most tests call handle() directly with made-up times, so they need no thread
 * and no waiting. One test at the end runs the real thread.
 */
class TrackerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final AtomicInteger closed = new AtomicInteger();

    // Stale after 2 s and given up on after 10 s, as in run/config.example.yaml.
    private final Tracker tracker = new Tracker(
            new StreamMachine(Duration.ofSeconds(2), Duration.ofSeconds(10)), START, closed::incrementAndGet);

    private static Instant t(long millis) {
        return START.plusMillis(millis);
    }

    private static StreamMessage message(long millis) {
        return new StreamMessage(t(millis));
    }

    private static StreamEnded ended(long millis) {
        return new StreamEnded(t(millis), "made up for the test");
    }

    private static Tick tick(long millis) {
        return new Tick(t(millis));
    }

    @Test
    void aStreamThatGoesAwayAndComesBackInTimeIsSurvived() {
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.ABSENT);

        tracker.handle(message(0));
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.LIVE);

        tracker.handle(tick(2_250));
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.STALE);

        tracker.handle(tick(10_250));
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.ABSENT);

        tracker.handle(message(15_000));
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.LIVE);
        assertThat(tracker.snapshot().lastMessage()).contains(t(15_000));
        assertThat(closed).hasValue(0);
    }

    @Test
    void aGameLeftClosedEndsTheProgramOnce() {
        tracker.handle(message(0));
        tracker.handle(ended(100));
        tracker.handle(tick(10_250));
        tracker.handle(tick(20_000));
        assertThat(closed).hasValue(0);

        tracker.handle(tick(20_251));
        assertThat(closed).hasValue(1);

        tracker.handle(tick(30_000));
        tracker.handle(message(31_000));
        assertThat(closed).hasValue(1);
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.ABSENT);
    }

    @Test
    void aProgramStartedBeforeTheGameNeverQuits() {
        Snapshot first = tracker.snapshot();

        tracker.handle(ended(0));
        tracker.handle(tick(250));
        tracker.handle(tick(3_600_000));

        assertThat(closed).hasValue(0);
        // Nothing changes while waiting, so nothing is published and nothing repaints.
        assertThat(tracker.snapshot()).isSameAs(first);
    }

    @Test
    void snapshotsArePublishedAtMostTenTimesASecond() {
        tracker.handle(message(0));
        Snapshot first = tracker.snapshot();

        tracker.handle(message(60));
        assertThat(tracker.snapshot()).isSameAs(first);

        tracker.handle(message(120));
        assertThat(tracker.snapshot().at()).isEqualTo(t(120));
        assertThat(tracker.snapshot().lastMessage()).contains(t(120));
    }

    @Test
    void aStaleSnapshotKeepsItsAgeUpToDate() {
        tracker.handle(message(0));
        tracker.handle(ended(100));
        assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.STALE);

        tracker.handle(tick(250));

        assertThat(tracker.snapshot().at()).isEqualTo(t(250));
        assertThat(tracker.snapshot().lastMessage()).contains(t(0));
    }

    @Test
    void theTrackerThreadTakesEventsFromTheQueue() throws InterruptedException {
        tracker.start();
        try {
            tracker.post(message(0));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (tracker.snapshot().stream() != StreamState.LIVE && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(tracker.snapshot().stream()).isEqualTo(StreamState.LIVE);
        } finally {
            tracker.stop();
        }
    }
}
