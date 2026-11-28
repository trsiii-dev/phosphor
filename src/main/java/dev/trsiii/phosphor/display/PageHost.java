package dev.trsiii.phosphor.display;

import dev.trsiii.phosphor.pages.Look;
import dev.trsiii.phosphor.tracker.Snapshot;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import javax.swing.JPanel;

/** The panel inside a window. It reads the newest snapshot and has it painted. */
final class PageHost extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ScreenPainter painter;
    private final transient Supplier<Snapshot> snapshots;
    private transient Snapshot shown;

    PageHost(ScreenPainter painter, Supplier<Snapshot> snapshots) {
        this.painter = painter;
        this.snapshots = snapshots;
        setBackground(Look.GROUND);
        // Same 3:4 shape as an MFD screen, small enough to fit several on a laptop.
        setPreferredSize(new Dimension(450, 600));
    }

    /** Repaints only if a newer snapshot has been published. Called by the repaint timer. */
    void refresh() {
        if (snapshots.get() != shown) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        shown = snapshots.get();
        Graphics2D copy = (Graphics2D) g.create();
        try {
            painter.paint(copy, shown, getSize());
        } finally {
            copy.dispose();
        }
    }
}
