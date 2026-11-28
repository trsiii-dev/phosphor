package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.telemetry.Gauge;
import dev.trsiii.phosphor.telemetry.Quantity;
import dev.trsiii.phosphor.view.Compass;
import dev.trsiii.phosphor.view.Messages;
import dev.trsiii.phosphor.view.Units;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.OptionalDouble;

/**
 * The compass in the middle of FLIGHT. It only draws: how far the ring turns is
 * decided by {@link Compass}.
 */
final class CompassRose {

    // Center of the ring, in design units.
    private static final double CX = 300;
    private static final double CY = 334;

    private CompassRose() {}

    /**
     * @param ringColor  color of the ring, its ticks and the fixed mark
     * @param valueColor color of the letters and the heading number
     */
    static void draw(Graphics2D g, Layout at, OptionalDouble heading, Color ringColor, Color valueColor) {
        OptionalDouble turn = Compass.ringTurn(heading);
        if (turn.isEmpty()) {
            drawUnknown(g, at);
            return;
        }

        // The fixed mark the heading is read against.
        Path2D mark = new Path2D.Double();
        mark.moveTo(at.x(300), at.y(233));
        mark.lineTo(at.x(291), at.y(215));
        mark.lineTo(at.x(309), at.y(215));
        mark.closePath();
        g.setColor(ringColor);
        g.fill(mark);

        // The ring, its ticks and the four letters turn together. A copy is used so
        // the rotation is gone again once it is disposed.
        Graphics2D ring = (Graphics2D) g.create();
        try {
            ring.rotate(Math.toRadians(turn.getAsDouble()), at.x(CX), at.y(CY));
            ring.setColor(ringColor);
            ring.setStroke(new BasicStroke((float) at.len(1.5)));
            ring.draw(new Ellipse2D.Double(at.x(CX - 103), at.y(CY - 103), at.len(206), at.len(206)));
            ring.setStroke(new BasicStroke((float) at.len(2)));
            for (int degrees = 0; degrees < 360; degrees += 10) {
                double sin = Math.sin(Math.toRadians(degrees));
                double cos = Math.cos(Math.toRadians(degrees));
                ring.draw(new Line2D.Double(
                        at.x(CX + 88 * sin), at.y(CY - 88 * cos), at.x(CX + 100 * sin), at.y(CY - 100 * cos)));
            }
            ring.setColor(valueColor);
            ring.setFont(Look.bold(at.len(20)));
            Text.center(ring, "N", at.x(300), at.y(270));
            Text.center(ring, "E", at.x(370), at.y(341));
            Text.center(ring, "S", at.x(300), at.y(412));
            Text.center(ring, "W", at.x(230), at.y(341));
        } finally {
            ring.dispose();
        }

        g.setColor(Look.DIM);
        g.setFont(Look.regular(at.len(13)));
        Text.center(g, Gauge.HEADING.label(), at.x(300), at.y(300));
        // The unit label is the same in every units setting.
        Text.center(g, Units.reading(heading, Quantity.ANGLE, UnitSystem.METRIC).unit(), at.x(300), at.y(378));
        g.setColor(valueColor);
        g.setFont(Look.bold(at.len(52)));
        Text.center(g, Units.reading(heading, Quantity.ANGLE, UnitSystem.METRIC).text(), at.x(300), at.y(352));
    }

    // A crossed box. A ring is never drawn for an unknown heading, because an
    // upright ring would read as north.
    private static void drawUnknown(Graphics2D g, Layout at) {
        Parts.outline(g, at, 220, 254, 160, 160, Look.DIM);
        g.draw(new Line2D.Double(at.x(220), at.y(254), at.x(380), at.y(414)));
        g.draw(new Line2D.Double(at.x(380), at.y(254), at.x(220), at.y(414)));
        g.setFont(Look.regular(at.len(13)));
        Text.center(g, Gauge.HEADING.label(), at.x(300), at.y(448));
        g.setFont(Look.bold(at.len(34)));
        Text.center(g, Messages.UNKNOWN, at.x(300), at.y(490));
    }
}
