package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.telemetry.Gauge;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.view.Formats;
import dev.trsiii.phosphor.view.Messages;
import dev.trsiii.phosphor.view.Units;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/** VITALS: the pilot's own values on top, the air around the pilot below. */
public final class VitalsPage implements MfdPage {

    // Two columns of five, left to right and top to bottom.
    private static final Gauge[] CELLS = {
        Gauge.HEALTH, Gauge.OXYGEN,
        Gauge.HUNGER, Gauge.THIRST,
        Gauge.BODY_TEMPERATURE, Gauge.HEART_RATE,
        Gauge.OUTSIDE_TEMPERATURE, Gauge.RADIATION,
        Gauge.PRESSURE, Gauge.GRAVITY
    };

    @Override
    public String name() {
        return "VITALS";
    }

    @Override
    public boolean needsStream() {
        return true;
    }

    @Override
    public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
        Layout at = Layout.of(size);
        Telemetry telemetry = snapshot.telemetry();
        Color color = Parts.valueColor(snapshot);

        Parts.header(g, at, snapshot, Messages.PILOT, telemetry.pilotState().orElse(Messages.UNKNOWN), color);

        // The air goes in the middle of the header, between the pilot and the data status.
        g.setColor(Look.DIM);
        g.setFont(Look.regular(at.len(14)));
        Text.center(g, Messages.AIR, at.x(300), at.y(56));
        g.setColor(color);
        g.setFont(Look.regular(at.len(20)));
        Text.center(
                g,
                Formats.either(telemetry.airBreathable(), Messages.BREATHABLE, Messages.NOT_BREATHABLE),
                at.x(300),
                at.y(82));

        for (int i = 0; i < CELLS.length; i++) {
            Gauge gauge = CELLS[i];
            double x = i % 2 == 0 ? 12 : 304;
            double y = 94 + (i / 2) * 130;
            Parts.wideCell(
                    g,
                    at,
                    x,
                    y,
                    gauge.label(),
                    Units.reading(telemetry.gauge(gauge), gauge.quantity(), snapshot.units()),
                    color);
        }
    }
}
