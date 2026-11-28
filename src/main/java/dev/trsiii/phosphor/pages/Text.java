package dev.trsiii.phosphor.pages;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Text drawing. Positions come from measured widths, so they hold for any font and size. */
public final class Text {

    private Text() {}

    /**
     * Draws text centered on centerX in the current font and color, wrapped at
     * spaces to fit maxWidth. Returns the baseline for the line after the last one.
     */
    public static float centered(
            Graphics2D g, String text, float centerX, float baseline, double maxWidth, double lineHeight) {
        FontMetrics metrics = g.getFontMetrics();
        float y = baseline;
        for (String line : wrap(text, metrics, maxWidth)) {
            g.drawString(line, centerX - metrics.stringWidth(line) / 2f, y);
            y += (float) lineHeight;
        }
        return y;
    }

    /** Draws one line centered on centerX. */
    public static void center(Graphics2D g, String text, float centerX, float baseline) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2f, baseline);
    }

    /**
     * Draws one line that ends at rightX. If it is wider than maxWidth, the font
     * is made smaller until it fits, so a long number never leaves its box.
     */
    public static void right(Graphics2D g, String text, float rightX, float baseline, double maxWidth) {
        Font font = g.getFont();
        int width = g.getFontMetrics().stringWidth(text);
        if (width > maxWidth && width > 0) {
            g.setFont(font.deriveFont((float) (font.getSize2D() * maxWidth / width)));
            width = g.getFontMetrics().stringWidth(text);
        }
        g.drawString(text, rightX - width, baseline);
        g.setFont(font);
    }

    /** Draws one line that starts at x, shrinking the font if it is wider than maxWidth. */
    public static void left(Graphics2D g, String text, float x, float baseline, double maxWidth) {
        Font font = g.getFont();
        int width = g.getFontMetrics().stringWidth(text);
        if (width > maxWidth && width > 0) {
            g.setFont(font.deriveFont((float) (font.getSize2D() * maxWidth / width)));
        }
        g.drawString(text, x, baseline);
        g.setFont(font);
    }

    // A single word wider than maxWidth still gets a line of its own.
    private static List<String> wrap(String text, FontMetrics metrics, double maxWidth) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String longer = line.isEmpty() ? word : line + " " + word;
            if (line.isEmpty() || metrics.stringWidth(longer) <= maxWidth) {
                line = longer;
            } else {
                lines.add(line);
                line = word;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }
}
