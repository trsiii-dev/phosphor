package dev.trsiii.phosphor.view;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.telemetry.Quantity;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Applies the units setting. Every measurement on every page goes through
 * {@link #reading}, so this is the only place that converts or rounds.
 *
 * <p>Values are converted first and rounded last. An unknown value is never
 * converted. It stays "---" and keeps its unit label.
 */
public final class Units {

    // Exact by definition: 1 ft = 0.3048 m, 1 mile = 1609.344 m, 1 nautical mile = 1852 m.
    private static final double FEET_PER_METER = 1 / 0.3048;
    private static final double MPH_PER_METER_PER_SECOND = 3600 / 1609.344;
    private static final double KNOTS_PER_METER_PER_SECOND = 3600.0 / 1852;
    private static final double FEET_PER_MINUTE_PER_METER_PER_SECOND = 60 / 0.3048;
    private static final double PSI_PER_ATMOSPHERE = 14.6959;
    private static final double HECTOPASCALS_PER_ATMOSPHERE = 1013.25;

    private Units() {}

    public static Reading reading(OptionalDouble value, Quantity quantity, UnitSystem units) {
        String unit = unit(quantity, units);
        if (value.isEmpty()) {
            return new Reading(Messages.UNKNOWN, unit);
        }
        return new Reading(format(convert(value.getAsDouble(), quantity, units), quantity, units), unit);
    }

    private static double convert(double value, Quantity quantity, UnitSystem units) {
        return switch (quantity) {
            case SPEED -> switch (units) {
                case METRIC -> value;
                case IMPERIAL -> value * MPH_PER_METER_PER_SECOND;
                case AVIATION -> value * KNOTS_PER_METER_PER_SECOND;
            };
            case VERTICAL_SPEED -> units == UnitSystem.METRIC ? value : value * FEET_PER_MINUTE_PER_METER_PER_SECOND;
            case LENGTH -> units == UnitSystem.METRIC ? value : value * FEET_PER_METER;
            // Not a single factor: 0 C is 32 F.
            case TEMPERATURE -> units == UnitSystem.IMPERIAL ? value * 9 / 5 + 32 : value;
            case PRESSURE -> switch (units) {
                case METRIC -> value;
                case IMPERIAL -> value * PSI_PER_ATMOSPHERE;
                case AVIATION -> value * HECTOPASCALS_PER_ATMOSPHERE;
            };
            case ANGLE -> ((value % 360) + 360) % 360;
            case PERCENT, G_FORCE, COUNT, RADIATION -> value;
        };
    }

    private static String unit(Quantity quantity, UnitSystem units) {
        return switch (quantity) {
            case SPEED -> switch (units) {
                case METRIC -> "M/S";
                case IMPERIAL -> "MPH";
                case AVIATION -> "KN";
            };
            case VERTICAL_SPEED -> units == UnitSystem.METRIC ? "M/S" : "FT/MIN";
            case LENGTH -> units == UnitSystem.METRIC ? "M" : "FT";
            case TEMPERATURE -> units == UnitSystem.IMPERIAL ? "F" : "C";
            case PRESSURE -> switch (units) {
                case METRIC -> "ATM";
                case IMPERIAL -> "PSI";
                case AVIATION -> "HPA";
            };
            case PERCENT -> "%";
            case G_FORCE -> "G";
            case ANGLE -> "DEG";
            case COUNT, RADIATION -> "";
        };
    }

    private static String format(double value, Quantity quantity, UnitSystem units) {
        return switch (quantity) {
            case G_FORCE, RADIATION -> oneDecimal(value);
            case PRESSURE -> units == UnitSystem.AVIATION ? whole(value) : oneDecimal(value);
            // The sign is always shown, so climbing and sinking read differently at a glance.
            case VERTICAL_SPEED -> {
                long rounded = Math.round(value);
                yield rounded > 0 ? "+" + rounded : Long.toString(rounded);
            }
            // Three digits, as on an aircraft instrument. A value that rounds to 360 is shown as 000.
            case ANGLE -> String.format(Locale.ROOT, "%03d", Math.round(value) % 360);
            case SPEED, LENGTH, PERCENT, TEMPERATURE, COUNT -> whole(value);
        };
    }

    private static String whole(double value) {
        return Long.toString(Math.round(value));
    }

    private static String oneDecimal(double value) {
        // Adding 0.0 turns a negative zero into zero, so -0.04 is shown as 0.0.
        return String.format(Locale.ROOT, "%.1f", Math.round(value * 10) / 10.0 + 0.0);
    }
}
