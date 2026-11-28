package dev.trsiii.phosphor.telemetry;

/** Every number a page can show, with its label and the kind of measurement it is. */
public enum Gauge {
    SPEED("SPEED", Quantity.SPEED),
    VERTICAL_SPEED("VERTICAL SPEED", Quantity.VERTICAL_SPEED),
    G_LOAD("G LOAD", Quantity.G_FORCE),
    BOOST_RESERVE("BOOST RESERVE", Quantity.PERCENT),
    ALTITUDE("ALTITUDE", Quantity.LENGTH),
    RADAR_ALTITUDE("RADAR ALTITUDE", Quantity.LENGTH),
    HYDROGEN_FUEL("HYDROGEN FUEL", Quantity.PERCENT),
    QUANTUM_FUEL("QUANTUM FUEL", Quantity.PERCENT),
    HEADING("HEADING", Quantity.ANGLE),

    HEALTH("HEALTH", Quantity.PERCENT),
    OXYGEN("OXYGEN", Quantity.PERCENT),
    HUNGER("HUNGER", Quantity.PERCENT),
    THIRST("THIRST", Quantity.PERCENT),
    BODY_TEMPERATURE("BODY TEMPERATURE", Quantity.TEMPERATURE),
    HEART_RATE("HEART RATE", Quantity.COUNT),
    OUTSIDE_TEMPERATURE("OUTSIDE TEMPERATURE", Quantity.TEMPERATURE),
    RADIATION("RADIATION", Quantity.RADIATION),
    PRESSURE("PRESSURE", Quantity.PRESSURE),
    GRAVITY("GRAVITY", Quantity.G_FORCE);

    private final String label;
    private final Quantity quantity;

    Gauge(String label, Quantity quantity) {
        this.label = label;
        this.quantity = quantity;
    }

    public String label() {
        return label;
    }

    public Quantity quantity() {
        return quantity;
    }
}
