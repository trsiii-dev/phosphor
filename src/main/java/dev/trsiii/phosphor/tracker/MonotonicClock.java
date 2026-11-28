package dev.trsiii.phosphor.tracker;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that only moves forward at a steady rate. Event times come from it,
 * so a correction of the system clock cannot make live data look stale or end
 * the program early.
 */
public final class MonotonicClock extends Clock {

    private final Instant base = Instant.now();
    private final long baseNanos = System.nanoTime();

    @Override
    public Instant instant() {
        return base.plusNanos(System.nanoTime() - baseNanos);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
