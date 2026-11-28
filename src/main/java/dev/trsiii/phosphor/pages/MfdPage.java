package dev.trsiii.phosphor.pages;

import dev.trsiii.phosphor.tracker.Snapshot;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * One page of information that fills one screen.
 *
 * <p>A page gets everything it needs as arguments and keeps nothing between
 * calls. It must not open connections, read files, or ask for its window, so it
 * can be painted into an off-screen image in tests.
 */
public interface MfdPage {

    /** Name used in the config file, in capitals. */
    String name();

    /**
     * True if the page has nothing to show without the stream. Such a page is
     * replaced by the Waiting page while the stream is absent.
     */
    default boolean needsStream() {
        return false;
    }

    /** Draws the snapshot into a rectangle of the given size, with its origin at 0,0. */
    void paint(Graphics2D g, Snapshot snapshot, Dimension size);
}
