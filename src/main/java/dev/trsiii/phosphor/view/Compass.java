package dev.trsiii.phosphor.view;

import java.util.OptionalDouble;

/** The arithmetic behind the compass on FLIGHT. The page only draws what this returns. */
public final class Compass {

    private Compass() {}

    /**
     * How far to turn the compass ring, in degrees, so that the heading sits under
     * the fixed mark at the top. Negative is counterclockwise. Empty if the heading
     * is unknown, in which case no ring may be drawn: an upright ring would read
     * as north.
     */
    public static OptionalDouble ringTurn(OptionalDouble heading) {
        if (heading.isEmpty() || !Double.isFinite(heading.getAsDouble())) {
            return OptionalDouble.empty();
        }
        double normalized = ((heading.getAsDouble() % 360) + 360) % 360;
        // Subtracting from zero avoids a negative zero for a heading of 0.
        return OptionalDouble.of(0.0 - normalized);
    }
}
