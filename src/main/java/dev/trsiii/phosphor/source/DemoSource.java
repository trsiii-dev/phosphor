package dev.trsiii.phosphor.source;

import dev.trsiii.phosphor.telemetry.Gauge;
import dev.trsiii.phosphor.telemetry.LightState;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.telemetry.WarningLight;
import dev.trsiii.phosphor.tracker.TrackerEvent;
import dev.trsiii.phosphor.tracker.TrackerEvent.StreamMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Stand-in for the game, for working on pages without it. It sends made-up
 * values for eight seconds, goes silent for five, and repeats, so the pages go
 * live, then stale, then live again. The silence is shorter than the give-up
 * time, so the program never quits on it.
 */
public final class DemoSource implements GameSource {

    // About 17 messages a second, like the game.
    private static final long MESSAGE_MILLIS = 60;
    private static final long LIVE_MILLIS = 8_000;
    private static final long SILENT_MILLIS = 5_000;

    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "phosphor-demo");
        thread.setDaemon(true);
        return thread;
    });

    public DemoSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void start(Consumer<TrackerEvent> sink) {
        Instant began = clock.instant();
        executor.scheduleAtFixedRate(
                () -> {
                    Instant now = clock.instant();
                    long elapsed = Duration.between(began, now).toMillis();
                    if (elapsed % (LIVE_MILLIS + SILENT_MILLIS) < LIVE_MILLIS) {
                        sink.accept(new StreamMessage(now, sample(elapsed / 1000.0)));
                    }
                },
                0,
                MESSAGE_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    /** The mock-ups' sample values, with a slow turn and a little movement so the pages are seen to update. */
    public static Telemetry sample(double seconds) {
        double wave = Math.sin(seconds / 3);

        Map<Gauge, Double> gauges = new EnumMap<>(Gauge.class);
        gauges.put(Gauge.SPEED, 212 + 8 * wave);
        gauges.put(Gauge.VERTICAL_SPEED, 12 * wave);
        gauges.put(Gauge.G_LOAD, 1.2 + 0.2 * wave);
        gauges.put(Gauge.BOOST_RESERVE, 100.0);
        gauges.put(Gauge.ALTITUDE, 4180 + 60 * wave);
        gauges.put(Gauge.RADAR_ALTITUDE, 1250 + 60 * wave);
        gauges.put(Gauge.HYDROGEN_FUEL, 82.0);
        gauges.put(Gauge.QUANTUM_FUEL, 64.0);
        gauges.put(Gauge.HEADING, (274 + seconds * 6) % 360);
        gauges.put(Gauge.HEALTH, 100.0);
        gauges.put(Gauge.OXYGEN, 100.0);
        gauges.put(Gauge.HUNGER, 64.0);
        gauges.put(Gauge.THIRST, 58.0);
        gauges.put(Gauge.BODY_TEMPERATURE, 37.0);
        gauges.put(Gauge.HEART_RATE, 72 + 4 * wave);
        gauges.put(Gauge.OUTSIDE_TEMPERATURE, -12.0);
        gauges.put(Gauge.RADIATION, 0.0);
        gauges.put(Gauge.PRESSURE, 1.0);
        gauges.put(Gauge.GRAVITY, 1.0);

        Map<WarningLight, LightState> lights = new EnumMap<>(WarningLight.class);
        for (WarningLight light : WarningLight.values()) {
            lights.put(light, LightState.NOMINAL);
        }
        lights.put(WarningLight.SHIELDS, LightState.MINOR);
        lights.put(WarningLight.HIT, LightState.MAJOR);
        lights.put(WarningLight.MISSILE, LightState.MAJOR);

        return new Telemetry(
                gauges,
                lights,
                Optional.of("COUPLED"),
                Optional.of(false),
                Optional.of(false),
                Optional.of("SEATED"),
                Optional.of(true));
    }
}
