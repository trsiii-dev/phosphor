package dev.trsiii.phosphor.pages;

import java.awt.Dimension;

/**
 * Maps the 600 by 800 design grid of the mock-ups onto the rectangle a page is
 * given. The grid keeps its shape and is centered, so a page looks the same at
 * any screen size.
 *
 * @param unit pixels per design unit
 * @param left x of the grid's left edge, in pixels
 * @param top  y of the grid's top edge, in pixels
 */
public record Layout(double unit, double left, double top) {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 800;

    public static Layout of(Dimension size) {
        double unit = Math.min(size.getWidth() / WIDTH, size.getHeight() / HEIGHT);
        return new Layout(unit, (size.getWidth() - WIDTH * unit) / 2, (size.getHeight() - HEIGHT * unit) / 2);
    }

    public float x(double designX) {
        return (float) (left + designX * unit);
    }

    public float y(double designY) {
        return (float) (top + designY * unit);
    }

    /** A length in design units, in pixels. Also used for font sizes. */
    public double len(double design) {
        return design * unit;
    }
}
