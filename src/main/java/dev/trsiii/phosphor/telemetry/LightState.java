package dev.trsiii.phosphor.telemetry;

/** State of one warning light. A light the game said nothing about has no state at all. */
public enum LightState {
    NOMINAL,
    MINOR,
    MAJOR
}
