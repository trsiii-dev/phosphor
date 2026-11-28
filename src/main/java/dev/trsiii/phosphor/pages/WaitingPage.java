package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamState;
import dev.trsiii.phosphor.view.Messages;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Shown when a screen has nothing else to show. It says why.
 *
 * <p>TODO: say "Not in a ship." when the stream is live and the pilot is on foot.
 */
public final class WaitingPage implements MfdPage {

    private final boolean pageError;

    public WaitingPage() {
        this(false);
    }

    private WaitingPage(boolean pageError) {
        this.pageError = pageError;
    }

    /** The form a screen falls back to when its own page failed to paint. */
    public static WaitingPage forPageError() {
        return new WaitingPage(true);
    }

    @Override
    public String name() {
        return "WAITING";
    }

    @Override
    public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
        Layout at = Layout.of(size);
        float center = at.x(Layout.WIDTH / 2.0);
        double width = at.len(540);
        double lineHeight = at.len(28);

        g.setColor(Look.BRIGHT);
        g.setFont(Look.bold(at.len(36)));
        // The narrower limit breaks the title into two lines, as in the mock-up.
        Text.centered(g, Messages.WAITING_TITLE, center, at.y(315), at.len(440), at.len(45));

        g.setFont(Look.regular(at.len(19)));
        float y = at.y(410);
        if (snapshot.stream() == StreamState.ABSENT) {
            y = Text.centered(g, Messages.STREAM_NOT_ANSWERING, center, y, width, lineHeight);
        }
        y = Text.centered(g, Messages.PAGES_APPEAR, center, y, width, lineHeight);
        y = Text.centered(g, Messages.THANKS, center, y + (float) at.len(19), width, lineHeight);

        if (pageError) {
            g.setColor(Look.ALERT);
            Text.centered(g, Messages.PAGE_ERROR, center, y + (float) at.len(19), width, lineHeight);
        }
    }
}
