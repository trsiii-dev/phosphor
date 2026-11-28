package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamState;
import dev.trsiii.phosphor.view.Formats;
import dev.trsiii.phosphor.view.Messages;
import dev.trsiii.phosphor.view.Reading;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * The pieces the stream pages are built from: the header, the value cells, and
 * the boxes around them. All coordinates are in design units of the 600 by 800 grid.
 */
final class Parts {

    private Parts() {}

    /** Bright while the stream is live, amber-brown once the values are stale. */
    static Color valueColor(Snapshot snapshot) {
        return snapshot.stream() == StreamState.STALE ? Look.STALE : Look.BRIGHT;
    }

    static Shape box(Layout at, double x, double y, double width, double height) {
        double corner = at.len(8);
        return new RoundRectangle2D.Double(at.x(x), at.y(y), at.len(width), at.len(height), corner, corner);
    }

    static void outline(Graphics2D g, Layout at, double x, double y, double width, double height, Color color) {
        outline(g, at, x, y, width, height, color, 1);
    }

    static void outline(
            Graphics2D g, Layout at, double x, double y, double width, double height, Color color, double stroke) {
        g.setColor(color);
        g.setStroke(new BasicStroke((float) at.len(stroke)));
        g.draw(box(at, x, y, width, height));
    }

    /** The top of a stream page: a label and value on the left, the data status on the right. */
    static void header(Graphics2D g, Layout at, Snapshot snapshot, String label, String value, Color color) {
        g.setColor(Look.DIM);
        g.setFont(Look.regular(at.len(14)));
        g.drawString(label, at.x(12), at.y(56));
        Text.right(g, Messages.DATA, at.x(588), at.y(56), at.len(200));

        g.setFont(Look.regular(at.len(20)));
        g.setColor(color);
        Text.left(g, value, at.x(12), at.y(82), at.len(250));
        g.setColor(valueColor(snapshot));
        Text.right(g, Formats.dataStatus(snapshot), at.x(588), at.y(82), at.len(200));
    }

    /**
     * FLIGHT's cell, 150 wide and 155 tall: a label, a large right-aligned number,
     * and its unit. A unit too long to sit beside the number goes on the line below.
     */
    static void narrowCell(Graphics2D g, Layout at, double x, double y, String label, Reading reading, Color color) {
        outline(g, at, x, y, 150, 155, Look.DIM);
        g.setFont(Look.regular(at.len(13)));
        Text.left(g, label, at.x(x + 10), at.y(y + 64), at.len(132));
        boolean beside = g.getFontMetrics().stringWidth(reading.unit()) <= at.len(26);
        if (beside) {
            g.drawString(reading.unit(), at.x(x + 118), at.y(y + 108));
        } else {
            Text.right(g, reading.unit(), at.x(x + 140), at.y(y + 132), at.len(130));
        }
        g.setColor(color);
        g.setFont(Look.bold(at.len(34)));
        Text.right(g, reading.text(), at.x(x + (beside ? 112 : 140)), at.y(y + 108), at.len(beside ? 102 : 130));
    }

    /** VITALS' cell, 284 wide and 122 tall: a label, and under it the number with its unit. */
    static void wideCell(Graphics2D g, Layout at, double x, double y, String label, Reading reading, Color color) {
        outline(g, at, x, y, 284, 122, Look.DIM);
        g.setFont(Look.regular(at.len(14)));
        g.drawString(label, at.x(x + 12), at.y(y + 50));
        g.setColor(color);
        g.setFont(Look.bold(at.len(36)));
        Text.left(g, reading.joined(), at.x(x + 12), at.y(y + 92), at.len(260));
    }

    /** A low cell with a label on the left and a word on the right, such as LANDING GEAR and UP. */
    static void wordCell(
            Graphics2D g, Layout at, double x, double y, double width, String label, String word, Color color) {
        outline(g, at, x, y, width, 62, Look.DIM);
        g.setFont(Look.regular(at.len(14)));
        g.drawString(label, at.x(x + 14), at.y(y + 36));
        g.setColor(color);
        g.setFont(Look.bold(at.len(28)));
        Text.right(g, word, at.x(x + width - 14), at.y(y + 41), at.len(120));
    }
}
