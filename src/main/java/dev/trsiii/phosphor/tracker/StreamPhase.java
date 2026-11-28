package dev.trsiii.phosphor.tracker;

import java.time.Instant;

/**
 * Phase of the stream machine, with the one timestamp that phase needs.
 *
 * <p>Pages only see the three {@link StreamState} values. The machine needs five
 * phases because ABSENT is split into Waiting, Gone, and Closed.
 *
 * <pre>
 * phase     page sees   timestamp kept
 * Waiting   ABSENT      none
 * Live      LIVE        arrival of the newest message
 * Stale     STALE       arrival of the newest message
 * Gone      ABSENT      when the stream was given up on
 * Closed    ABSENT      none (the program is ending)
 * </pre>
 */
public sealed interface StreamPhase {

    /** State shown to pages during this phase. */
    StreamState state();

    /** True only in {@link Closed}, when the program should exit. */
    default boolean gameHasClosed() {
        return this instanceof Closed;
    }

    /** No message yet in this run. There is no timeout because the program can be started before the game. */
    record Waiting() implements StreamPhase {
        @Override
        public StreamState state() {
            return StreamState.ABSENT;
        }
    }

    /**
     * Messages are arriving.
     *
     * @param lastMessage arrival time of the newest message
     */
    record Live(Instant lastMessage) implements StreamPhase {
        @Override
        public StreamState state() {
            return StreamState.LIVE;
        }
    }

    /**
     * The stream ended or has been quiet for longer than {@code staleAfter}.
     *
     * @param lastMessage arrival time of the newest message, from which the age of the values is counted
     */
    record Stale(Instant lastMessage) implements StreamPhase {
        @Override
        public StreamState state() {
            return StreamState.STALE;
        }
    }

    /**
     * The stream was seen in this run and has been given up on. A new message
     * makes it live again.
     *
     * @param since when the stream was given up on, from which the wait for the game to return is counted
     */
    record Gone(Instant since) implements StreamPhase {
        @Override
        public StreamState state() {
            return StreamState.ABSENT;
        }
    }

    /** The stream stayed away, so the game has closed and the program ends. Terminal phase. */
    record Closed() implements StreamPhase {
        @Override
        public StreamState state() {
            return StreamState.ABSENT;
        }
    }
}
