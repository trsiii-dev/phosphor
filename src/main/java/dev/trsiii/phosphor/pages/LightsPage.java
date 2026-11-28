package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.telemetry.LightState;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.telemetry.WarningLight;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamState;
import dev.trsiii.phosphor.view.Formats;
import dev.trsiii.phosphor.view.Messages;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;

/** LIGHTS: the ten warning lights as tiles, with the landing gear and doors under them. */
public final class LightsPage implements MfdPage {

    @Override
    public String name() {
        return "LIGHTS";
    }

    @Override
    public boolean needsStream() {
        return true;
    }

    @Override
    public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
        Layout at = Layout.of(size);
        Telemetry telemetry = snapshot.telemetry();
        boolean stale = snapshot.stream() == StreamState.STALE;
        Color color = Parts.valueColor(snapshot);
        Color alert = stale ? Look.STALE : Look.ALERT;

        int lit = Formats.litCount(telemetry);
        Parts.header(g, at, snapshot, Messages.WARNING_LIGHTS, lit + Messages.LIT, lit > 0 ? alert : color);

        // Two columns of five, in the order of the enum.
        WarningLight[] lights = WarningLight.values();
        for (int i = 0; i < lights.length; i++) {
            double x = i % 2 == 0 ? 12 : 304;
            double y = 94 + (i / 2) * 116;
            tile(g, at, x, y, lights[i].label(), telemetry.light(lights[i]), color, alert);
        }

        Parts.wordCell(
                g,
                at,
                12,
                676,
                284,
                Messages.LANDING_GEAR,
                Formats.either(telemetry.gearDown(), Messages.GEAR_DOWN, Messages.GEAR_UP),
                color);
        Parts.wordCell(
                g,
                at,
                304,
                676,
                284,
                Messages.DOORS,
                Formats.either(telemetry.doorsOpen(), Messages.DOORS_OPEN, Messages.DOORS_CLOSED),
                color);
    }

    /**
     * One light, 284 wide and 108 tall. Nominal is a dim outline, a minor warning
     * is an outline in the alert color, and a major warning is a filled tile. A
     * light with no known state is dim and says so. It is never drawn as nominal.
     */
    private static void tile(
            Graphics2D g,
            Layout at,
            double x,
            double y,
            String name,
            Optional<LightState> state,
            Color color,
            Color alert) {
        Color nameColor;
        Color stateColor;
        String stateText;
        if (state.isEmpty()) {
            Parts.outline(g, at, x, y, 284, 108, Look.DIM);
            nameColor = Look.DIM;
            stateColor = Look.DIM;
            stateText = Messages.UNKNOWN;
        } else {
            switch (state.get()) {
                case MINOR -> {
                    Parts.outline(g, at, x, y, 284, 108, alert, 2);
                    nameColor = alert;
                    stateColor = alert;
                    stateText = Messages.MINOR;
                }
                case MAJOR -> {
                    g.setColor(alert);
                    g.fill(Parts.box(at, x, y, 284, 108));
                    nameColor = Look.GROUND;
                    stateColor = Look.GROUND;
                    stateText = Messages.MAJOR;
                }
                default -> {
                    Parts.outline(g, at, x, y, 284, 108, Look.DIM);
                    nameColor = color;
                    stateColor = Look.DIM;
                    stateText = Messages.NOMINAL;
                }
            }
        }
        g.setColor(nameColor);
        g.setFont(Look.bold(at.len(24)));
        Text.left(g, name, at.x(x + 14), at.y(y + 50), at.len(256));
        g.setColor(stateColor);
        g.setFont(Look.regular(at.len(14)));
        g.drawString(stateText, at.x(x + 14), at.y(y + 78));
    }
}
