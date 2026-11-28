package dev.trsiii.phosphor.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.telemetry.Quantity;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UnitsTest {

    // Each row: the value as the game gives it, its kind, the setting, then the text and the unit label shown.
    @ParameterizedTest
    @CsvSource({
        "100, SPEED, METRIC, 100, M/S",
        "100, SPEED, IMPERIAL, 224, MPH",
        "100, SPEED, AVIATION, 194, KN",
        "10, VERTICAL_SPEED, METRIC, +10, M/S",
        "10, VERTICAL_SPEED, IMPERIAL, +1969, FT/MIN",
        "-10, VERTICAL_SPEED, AVIATION, -1969, FT/MIN",
        "0, VERTICAL_SPEED, METRIC, 0, M/S",
        "1000, LENGTH, METRIC, 1000, M",
        "1000, LENGTH, IMPERIAL, 3281, FT",
        "1000, LENGTH, AVIATION, 3281, FT",
        "0, TEMPERATURE, METRIC, 0, C",
        "0, TEMPERATURE, IMPERIAL, 32, F",
        "37, TEMPERATURE, IMPERIAL, 99, F",
        "-40, TEMPERATURE, IMPERIAL, -40, F",
        "37, TEMPERATURE, AVIATION, 37, C",
        "1, PRESSURE, METRIC, 1.0, ATM",
        "1, PRESSURE, IMPERIAL, 14.7, PSI",
        "1, PRESSURE, AVIATION, 1013, HPA",
        "82.4, PERCENT, IMPERIAL, 82, %",
        "1.44, G_FORCE, METRIC, 1.4, G",
        "-0.04, G_FORCE, METRIC, 0.0, G",
        "274, ANGLE, METRIC, 274, DEG",
        "4, ANGLE, IMPERIAL, 004, DEG",
        "359.6, ANGLE, METRIC, 000, DEG",
        "-90, ANGLE, METRIC, 270, DEG",
        "72.4, COUNT, METRIC, 72, ''",
        "0.04, RADIATION, AVIATION, 0.0, ''"
    })
    void aValueIsConvertedThenRounded(double value, Quantity quantity, UnitSystem units, String text, String unit) {
        assertThat(Units.reading(OptionalDouble.of(value), quantity, units)).isEqualTo(new Reading(text, unit));
    }

    @Test
    void anUnknownValueStaysUnknownAndKeepsItsUnit() {
        for (Quantity quantity : Quantity.values()) {
            for (UnitSystem units : UnitSystem.values()) {
                Reading known = Units.reading(OptionalDouble.of(1), quantity, units);
                Reading unknown = Units.reading(OptionalDouble.empty(), quantity, units);

                assertThat(unknown.text()).isEqualTo("---");
                assertThat(unknown.unit()).isEqualTo(known.unit());
            }
        }
    }

    @Test
    void aReadingJoinsItsTextAndUnit() {
        assertThat(new Reading("37", "C").joined()).isEqualTo("37 C");
        assertThat(new Reading("72", "").joined()).isEqualTo("72");
    }
}
