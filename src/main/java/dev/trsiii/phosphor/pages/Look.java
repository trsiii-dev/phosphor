package dev.trsiii.phosphor.pages;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

/** Colors and fonts shared by every page. */
public final class Look {

    /** The screen's background. */
    public static final Color GROUND = new Color(0x04070A);

    /** Values. */
    public static final Color BRIGHT = new Color(0x6CFFB0);

    /** Labels and outlines. */
    public static final Color DIM = new Color(0x2E8F63);

    /** Something needs attention. */
    public static final Color ALERT = new Color(0xFFB347);

    /** Stale values, which should not be trusted. */
    public static final Color STALE = new Color(0xB8853A);

    /** The compass ring and its mark on FLIGHT. Redder than ALERT so the two are not confused. */
    public static final Color COMPASS = new Color(0xFF8C1A);

    private static final Font REGULAR = load("B612Mono-Regular.ttf", Font.PLAIN);
    private static final Font BOLD = load("B612Mono-Bold.ttf", Font.BOLD);

    private Look() {}

    public static Font regular(double size) {
        return REGULAR.deriveFont((float) size);
    }

    public static Font bold(double size) {
        return BOLD.deriveFont((float) size);
    }

    // B612 Mono if its file is on the classpath, otherwise the JDK's monospaced font.
    // TODO: bundle the font files once the license question is settled.
    private static Font load(String file, int fallbackStyle) {
        try (InputStream in = Look.class.getResourceAsStream("/dev/trsiii/phosphor/fonts/" + file)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (IOException | FontFormatException e) {
            // Fall through to the built-in font.
        }
        return new Font(Font.MONOSPACED, fallbackStyle, 1);
    }
}
