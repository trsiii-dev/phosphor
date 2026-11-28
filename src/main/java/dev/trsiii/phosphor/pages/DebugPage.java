package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.view.Messages;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Development only. Lists every field of the snapshot. */
public final class DebugPage implements MfdPage {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS 'UTC'").withZone(ZoneOffset.UTC);

    @Override
    public String name() {
        return "DEBUG";
    }

    @Override
    public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
        Layout at = Layout.of(size);
        row(g, at, 0, "STREAM", snapshot.stream().name());
        row(g, at, 1, "SNAPSHOT MADE", TIME.format(snapshot.at()));
        row(g, at, 2, "LAST MESSAGE", snapshot.lastMessage()
                .map(arrived -> age(arrived, snapshot.at()))
                .orElse(Messages.UNKNOWN));
    }

    private static void row(Graphics2D g, Layout at, int index, String label, String value) {
        double top = 60 + index * 80;
        g.setColor(Look.DIM);
        g.setFont(Look.regular(at.len(14)));
        g.drawString(label, at.x(14), at.y(top));
        g.setColor(Look.BRIGHT);
        g.setFont(Look.bold(at.len(24)));
        g.drawString(value, at.x(14), at.y(top + 32));
    }

    // Age at the moment the snapshot was made. A page has no clock of its own.
    private static String age(Instant then, Instant snapshotTime) {
        double seconds = Duration.between(then, snapshotTime).toMillis() / 1000.0;
        return String.format(Locale.ROOT, "%.1f S OLD", seconds);
    }
}
