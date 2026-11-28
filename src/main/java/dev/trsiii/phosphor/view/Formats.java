package dev.trsiii.phosphor.view;

import dev.trsiii.phosphor.telemetry.LightState;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.telemetry.WarningLight;
import dev.trsiii.phosphor.tracker.Snapshot;
import java.time.Duration;
import java.util.Optional;

/** Turns snapshot values into the text a page shows. */
public final class Formats {

    private Formats() {}

    /** The data status in a page's header: LIVE, the age of stale values, or NO STREAM. */
    public static String dataStatus(Snapshot snapshot) {
        return switch (snapshot.stream()) {
            case LIVE -> Messages.LIVE;
            case STALE -> snapshot.lastMessage()
                    .map(arrived -> Duration.between(arrived, snapshot.at()).toSeconds() + " S OLD")
                    .orElse(Messages.UNKNOWN);
            case ABSENT -> Messages.NO_STREAM;
        };
    }

    /** Number of warning lights that are lit. A light with no known state is not counted. */
    public static int litCount(Telemetry telemetry) {
        int lit = 0;
        for (WarningLight light : WarningLight.values()) {
            Optional<LightState> state = telemetry.light(light);
            if (state.isPresent() && state.get() != LightState.NOMINAL) {
                lit++;
            }
        }
        return lit;
    }

    /** One of two words for a yes-or-no value, or "---" if it is unknown. */
    public static String either(Optional<Boolean> value, String whenTrue, String whenFalse) {
        return value.map(flag -> flag ? whenTrue : whenFalse).orElse(Messages.UNKNOWN);
    }
}
