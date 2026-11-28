package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.telemetry.Gauge;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamState;
import dev.trsiii.phosphor.view.Formats;
import dev.trsiii.phosphor.view.Messages;
import dev.trsiii.phosphor.view.Units;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/** FLIGHT: speed, altitude and fuel in two columns, with the compass between them. */
public final class FlightPage implements MfdPage {

    private static final Gauge[] LEFT = {Gauge.SPEED, Gauge.VERTICAL_SPEED, Gauge.G_LOAD, Gauge.BOOST_RESERVE};
    private static final Gauge[] RIGHT = {
        Gauge.ALTITUDE, Gauge.RADAR_ALTITUDE, Gauge.HYDROGEN_FUEL, Gauge.QUANTUM_FUEL
    };

    // Top edge of each of the four rows, in design units.
    private static final double[] ROWS = {94, 257, 420, 583};

    @Override
    public String name() {
        return "FLIGHT";
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

        Parts.header(
                g, at, snapshot, Messages.FLIGHT_MODE, telemetry.flightMode().orElse(Messages.UNKNOWN), color);

        for (int row = 0; row < ROWS.length; row++) {
            cell(g, at, 12, ROWS[row], LEFT[row], snapshot, color);
            cell(g, at, 438, ROWS[row], RIGHT[row], snapshot, color);
        }

        Parts.outline(g, at, 170, 94, 260, 481, Look.DIM);
        Color ringColor = snapshot.stream() == StreamState.STALE ? Look.STALE : Look.COMPASS;
        CompassRose.draw(g, at, telemetry.gauge(Gauge.HEADING), ringColor, color);

        // The landing gear cell under the compass, the same height as the cells beside it.
        Parts.outline(g, at, 170, 583, 260, 155, Look.DIM);
        g.setFont(Look.regular(at.len(13)));
        g.drawString(Messages.LANDING_GEAR, at.x(182), at.y(647));
        g.setColor(color);
        g.setFont(Look.bold(at.len(34)));
        Text.right(
                g,
                Formats.either(telemetry.gearDown(), Messages.GEAR_DOWN, Messages.GEAR_UP),
                at.x(418),
                at.y(691),
                at.len(236));
    }

    private static void cell(
            Graphics2D g, Layout at, double x, double y, Gauge gauge, Snapshot snapshot, Color color) {
        Parts.narrowCell(
                g,
                at,
                x,
                y,
                gauge.label(),
                Units.reading(snapshot.telemetry().gauge(gauge), gauge.quantity(), snapshot.units()),
                color);
    }
}
