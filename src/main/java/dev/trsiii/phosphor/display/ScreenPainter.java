package dev.trsiii.phosphor.display;

import dev.trsiii.phosphor.pages.Layout;
import dev.trsiii.phosphor.pages.Look;
import dev.trsiii.phosphor.pages.MfdPage;
import dev.trsiii.phosphor.pages.WaitingPage;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamState;
import dev.trsiii.phosphor.view.Messages;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Paints one screen: the background, the page, and the footer.
 *
 * <p>The footer is drawn here so that no page has to know the build number. A
 * page that throws is replaced by the Waiting page with a "page error" line, so
 * one broken page cannot take the other screens down.
 *
 * <p>This class has no window. The tests paint through it into an image.
 */
public final class ScreenPainter {

    private static final System.Logger LOG = System.getLogger(ScreenPainter.class.getName());
    private static final MfdPage WAITING_PAGE = new WaitingPage();
    private static final MfdPage ERROR_PAGE = WaitingPage.forPageError();

    private final MfdPage page;
    private final boolean defaultPage;
    private final String build;
    private boolean failureLogged;

    /**
     * @param defaultPage true if the page is the screen's default page, which the footer says
     * @param build       build number for the footer
     */
    public ScreenPainter(MfdPage page, boolean defaultPage, String build) {
        this.page = page;
        this.defaultPage = defaultPage;
        this.build = build;
    }

    public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
        if (size.width < 1 || size.height < 1) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // A page that reads the stream has nothing to show without it.
        boolean waiting = page.needsStream() && snapshot.stream() == StreamState.ABSENT;

        clear(g, size);
        if (!paintPage(waiting ? WAITING_PAGE : page, g, snapshot, size)) {
            // Wipe whatever the page drew before it failed.
            clear(g, size);
            paintPage(ERROR_PAGE, g, snapshot, size);
        }

        // The footer calls the page the default only while that page is the one showing.
        Layout at = Layout.of(size);
        g.setColor(Look.DIM);
        g.setFont(Look.regular(at.len(13)));
        g.drawString(Messages.footer(build, defaultPage && !waiting), at.x(14), at.y(784));
    }

    private static void clear(Graphics2D g, Dimension size) {
        g.setColor(Look.GROUND);
        g.fillRect(0, 0, size.width, size.height);
    }

    private boolean paintPage(MfdPage target, Graphics2D g, Snapshot snapshot, Dimension size) {
        // The page gets a copy, so it cannot leave a color, font or transform behind.
        Graphics2D copy = (Graphics2D) g.create();
        try {
            target.paint(copy, snapshot, size);
            return true;
        } catch (RuntimeException e) {
            // Logged once. A page that fails would otherwise log on every repaint.
            if (!failureLogged) {
                failureLogged = true;
                LOG.log(System.Logger.Level.ERROR, "Page " + page.name() + " failed to paint.", e);
            }
            return false;
        } finally {
            copy.dispose();
        }
    }
}
