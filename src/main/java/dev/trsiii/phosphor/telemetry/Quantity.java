package dev.trsiii.phosphor.telemetry;

/** The kind of measurement a gauge holds. The units setting decides how each kind is shown. */
public enum Quantity {
    /** Meters per second. */
    SPEED,
    /** Meters per second, shown with its sign. */
    VERTICAL_SPEED,
    /** Meters. */
    LENGTH,
    PERCENT,
    /** Multiples of standard gravity. */
    G_FORCE,
    /** Degrees Celsius. */
    TEMPERATURE,
    /** Standard atmospheres. */
    PRESSURE,
    /** Degrees, 0 to 360. */
    ANGLE,
    /** A plain whole number with no unit, such as beats per minute. */
    COUNT,
    /** The game's own scale, shown with one decimal and no unit. */
    RADIATION
}
