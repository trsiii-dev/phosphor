package dev.trsiii.phosphor.telemetry;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The ship and pilot values from one stream message, in the program's own
 * form. The stream client fills this in, so the pages do not depend on the
 * stream's message definitions.
 *
 * <p>A value the game did not send is absent. It is never stored as zero,
 * because a zero would be drawn as a real reading.
 *
 * @param gauges        numbers, in the units listed on {@link Quantity}
 * @param lights        warning lights that have a known state
 * @param flightMode    for example COUPLED
 * @param gearDown      landing gear
 * @param doorsOpen     true if any door is open
 * @param pilotState    for example SEATED
 * @param airBreathable the air around the pilot
 */
public record Telemetry(
        Map<Gauge, Double> gauges,
        Map<WarningLight, LightState> lights,
        Optional<String> flightMode,
        Optional<Boolean> gearDown,
        Optional<Boolean> doorsOpen,
        Optional<String> pilotState,
        Optional<Boolean> airBreathable) {

    /** Nothing known. */
    public static final Telemetry EMPTY = new Telemetry(
            Map.of(), Map.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public Telemetry {
        gauges = Map.copyOf(gauges);
        lights = Map.copyOf(lights);
    }

    /** Empty if the gauge is missing or is not a finite number. */
    public OptionalDouble gauge(Gauge gauge) {
        Double value = gauges.get(gauge);
        return value == null || !Double.isFinite(value) ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public Optional<LightState> light(WarningLight light) {
        return Optional.ofNullable(lights.get(light));
    }
}
