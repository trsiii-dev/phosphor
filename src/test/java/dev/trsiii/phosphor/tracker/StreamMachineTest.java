package dev.trsiii.phosphor.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Transition table for StreamMachine, with one row per transition and one per
 * transition that must not exist. Events carry their own time, so these tests
 * need no clock and no sleeping.
 */
class StreamMachineTest {

    // Same values as run/config.example.yaml (staleAfter 2 s, giveUpAfter 10 s).
    private static final StreamMachine MACHINE = new StreamMachine(Duration.ofSeconds(2), Duration.ofSeconds(10));

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

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

    /** Each row is a description, the phase before, the event, and the phase after. */
    static Stream<Arguments> arrows() {
        return Stream.of(
                // Transitions that exist.
                arguments("ABSENT to LIVE on a message", new Waiting(), message(5_000), new Live(t(5_000))),
                arguments("LIVE stays LIVE and keeps the newest time", new Live(t(0)), message(60), new Live(t(60))),
                arguments("LIVE to STALE when the stream ends", new Live(t(0)), ended(500), new Stale(t(0))),
                arguments(
                        "LIVE to STALE when the newest message is older than staleAfter",
                        new Live(t(0)),
                        tick(2_001),
                        new Stale(t(0))),
                arguments("STALE to LIVE on the next message", new Stale(t(0)), message(3_000), new Live(t(3_000))),
                arguments(
                        "STALE to ABSENT when the newest message is older than giveUpAfter",
                        new Stale(t(0)),
                        tick(10_001),
                        new Gone(t(10_001))),
                arguments(
                        "ABSENT back to LIVE when the game returns in time",
                        new Gone(t(10_001)),
                        message(15_000),
                        new Live(t(15_000))),
                arguments(
                        "the exit: seen, then ABSENT for longer than giveUpAfter",
                        new Gone(t(10_001)),
                        tick(20_002),
                        new Closed()),

                // Transitions that must not exist.
                arguments("never seen: no tick ends the program", new Waiting(), tick(86_400_000), new Waiting()),
                arguments("never seen: a failed connection changes nothing", new Waiting(), ended(5_000), new Waiting()),
                arguments("LIVE at exactly staleAfter is not stale yet", new Live(t(0)), tick(2_000), new Live(t(0))),
                arguments(
                        "LIVE never jumps to ABSENT: it passes through STALE",
                        new Live(t(0)),
                        tick(60_000),
                        new Stale(t(0))),
                arguments(
                        "STALE at exactly giveUpAfter is not absent yet",
                        new Stale(t(0)),
                        tick(10_000),
                        new Stale(t(0))),
                arguments(
                        "STALE is not changed by the stream ending again",
                        new Stale(t(0)),
                        ended(3_000),
                        new Stale(t(0))),
                arguments(
                        "ABSENT at exactly giveUpAfter does not end the program yet",
                        new Gone(t(10_000)),
                        tick(20_000),
                        new Gone(t(10_000))),
                arguments(
                        "ABSENT: a failed reconnection does not restart the count",
                        new Gone(t(10_000)),
                        ended(15_000),
                        new Gone(t(10_000))),
                arguments("the end is the end: a message", new Closed(), message(30_000), new Closed()),
                arguments("the end is the end: the stream ending", new Closed(), ended(30_000), new Closed()),
                arguments("the end is the end: a tick", new Closed(), tick(30_000), new Closed()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arrows")
    void everyTransitionMatchesTheTable(String arrow, StreamPhase before, TrackerEvent event, StreamPhase after) {
        assertThat(MACHINE.next(before, event)).isEqualTo(after);
    }

    @Test
    void aPageSeesThreeStatesHoweverManyPhasesThereAre() {
        assertThat(new Waiting().state()).isEqualTo(StreamState.ABSENT);
        assertThat(new Live(t(0)).state()).isEqualTo(StreamState.LIVE);
        assertThat(new Stale(t(0)).state()).isEqualTo(StreamState.STALE);
        assertThat(new Gone(t(0)).state()).isEqualTo(StreamState.ABSENT);
        assertThat(new Closed().state()).isEqualTo(StreamState.ABSENT);
    }

    @Test
    void onlyTheLastPhaseEndsTheProgram() {
        assertThat(new Waiting().gameHasClosed()).isFalse();
        assertThat(new Live(t(0)).gameHasClosed()).isFalse();
        assertThat(new Stale(t(0)).gameHasClosed()).isFalse();
        assertThat(new Gone(t(0)).gameHasClosed()).isFalse();
        assertThat(new Closed().gameHasClosed()).isTrue();
    }

    @Test
    void aGameThatIsQuitAndLeftClosedEndsTheProgram() {
        StreamPhase phase = walk(
                message(0),
                message(60),
                ended(100), // game quit, STALE counted from the message at 60
                tick(250), // still STALE
                tick(10_250), // newest message older than 10 s, so ABSENT
                tick(20_000), // ABSENT for 9.75 s, not closed yet
                tick(20_251)); // ABSENT for more than 10 s

        assertThat(phase).isEqualTo(new Closed());
    }

    @Test
    void aGameRestartedInsideTheGiveUpTimeIsSurvived() {
        StreamPhase phase = walk(
                message(0),
                ended(100),
                tick(10_250), // ABSENT
                ended(15_000), // reconnect attempt fails
                message(18_000)); // game is back

        assertThat(phase).isEqualTo(new Live(t(18_000)));
    }

    @Test
    void aProgramStartedBeforeTheGameWaitsForAsLongAsItTakes() {
        StreamPhase phase = walk(ended(0), tick(250), ended(5_000), tick(3_600_000));

        assertThat(phase).isEqualTo(new Waiting());
    }

    /** Runs the events through the machine from its start phase and returns the final phase. */
    private static StreamPhase walk(TrackerEvent... events) {
        StreamPhase phase = MACHINE.start();
        for (TrackerEvent event : events) {
            phase = MACHINE.next(phase, event);
        }
        return phase;
    }
}
