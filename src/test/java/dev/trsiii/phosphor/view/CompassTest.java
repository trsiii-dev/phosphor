package dev.trsiii.phosphor.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompassTest {

    // Heading, then how far the ring turns. The ring turns against the heading.
    @ParameterizedTest
    @CsvSource({"0, 0", "90, -90", "180, -180", "274, -274", "359.5, -359.5", "360, 0", "450, -90", "-90, -270"})
    void theRingTurnsAgainstTheHeading(double heading, double turn) {
        assertThat(Compass.ringTurn(OptionalDouble.of(heading)).getAsDouble()).isEqualTo(turn);
    }

    @Test
    void anUnknownHeadingHasNoRing() {
        assertThat(Compass.ringTurn(OptionalDouble.empty())).isEmpty();
        assertThat(Compass.ringTurn(OptionalDouble.of(Double.NaN))).isEmpty();
    }
}
