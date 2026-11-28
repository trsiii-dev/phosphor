package dev.trsiii.phosphor.telemetry;

/** The ten warning lights of the ship's annunciator panel, in the order LIGHTS shows them. */
public enum WarningLight {
    POWER("POWER"),
    WEAPONS("WEAPONS"),
    THRUSTERS("THRUSTERS"),
    SHIELDS("SHIELDS"),
    COOLERS("COOLERS"),
    QUANTUM_DRIVE("QUANTUM DRIVE"),
    RADAR("RADAR"),
    PROXIMITY("PROXIMITY"),
    HIT("HIT"),
    MISSILE("MISSILE");

    private final String label;

    WarningLight(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
