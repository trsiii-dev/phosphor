package dev.trsiii.phosphor.display;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.trsiii.phosphor.pages.DebugPage;
import dev.trsiii.phosphor.pages.Look;
import dev.trsiii.phosphor.pages.MfdPage;
import dev.trsiii.phosphor.pages.WaitingPage;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamPhase;
import dev.trsiii.phosphor.view.Messages;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pages are painted into an image, so these tests need no window and no display. */
class ScreenPainterTest {

    // One design unit is one pixel at this size.
    private static final Dimension SIZE = new Dimension(600, 800);

    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final Snapshot WAITING = Snapshot.of(new StreamPhase.Waiting(), TIME);
    private static final Snapshot LIVE = Snapshot.of(new StreamPhase.Live(TIME), TIME.plusMillis(300));

    private static final MfdPage BROKEN = new MfdPage() {
        @Override
        public String name() {
            return "BROKEN";
        }

        @Override
        public void paint(Graphics2D g, Snapshot snapshot, Dimension size) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size.width, size.height);
            throw new IllegalStateException("made to fail");
        }
    };

    private static BufferedImage paint(ScreenPainter painter, Snapshot snapshot, Dimension size) {
        BufferedImage image =
                new BufferedImage(Math.max(1, size.width), Math.max(1, size.height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            painter.paint(g, snapshot, size);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Number of pixels in the rectangle that are not the background color. */
    private static long inked(BufferedImage image, int x, int y, int width, int height) {
        int ground = Look.GROUND.getRGB();
        long count = 0;
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                if (image.getRGB(column, row) != ground) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long footerInk(BufferedImage image) {
        return inked(image, 0, 765, 600, 30);
    }

    private static boolean hasOrange(BufferedImage image) {
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                Color color = new Color(image.getRGB(column, row));
                if (color.getRed() > 120 && color.getRed() - color.getGreen() > 30 && color.getBlue() < 130) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void theWaitingPageDrawsItsText() {
        BufferedImage image = paint(new ScreenPainter(new WaitingPage(), false, "1.1"), WAITING, SIZE);

        assertThat(inked(image, 0, 280, 600, 230)).isPositive();
        assertThat(image.getRGB(0, 0)).isEqualTo(Look.GROUND.getRGB());
        assertThat(hasOrange(image)).isFalse();
    }

    @Test
    void theDebugPageDrawsTheSnapshot() {
        BufferedImage image = paint(new ScreenPainter(new DebugPage(), false, "1.1"), LIVE, SIZE);

        assertThat(inked(image, 0, 30, 600, 250)).isPositive();
    }

    @Test
    void everyPageGetsAFooter() {
        assertThat(footerInk(paint(new ScreenPainter(new WaitingPage(), false, "1.1"), WAITING, SIZE)))
                .isPositive();
        assertThat(footerInk(paint(new ScreenPainter(new DebugPage(), false, "1.1"), LIVE, SIZE)))
                .isPositive();
    }

    @Test
    void aDefaultPageSaysSoInTheFooter() {
        long plain = footerInk(paint(new ScreenPainter(new WaitingPage(), false, "1.1"), WAITING, SIZE));
        long marked = footerInk(paint(new ScreenPainter(new WaitingPage(), true, "1.1"), WAITING, SIZE));

        assertThat(marked).isGreaterThan(plain);
        assertThat(Messages.footer("1.1", false)).isEqualTo("PHOSPHOR 1.1");
        assertThat(Messages.footer("1.1", true)).isEqualTo("DEFAULT · PHOSPHOR 1.1");
    }

    @Test
    void aPageThatThrowsIsReplacedByThePageError() {
        ScreenPainter painter = new ScreenPainter(BROKEN, true, "1.1");

        BufferedImage image = paint(painter, WAITING, SIZE);

        // The broken page filled the screen before it failed. That must be wiped.
        assertThat(image.getRGB(0, 0)).isEqualTo(Look.GROUND.getRGB());
        assertThat(hasOrange(image)).isTrue();
        assertThat(footerInk(image)).isPositive();
        // A second paint must work too. The failure is only logged once.
        assertThatCode(() -> paint(painter, WAITING, SIZE)).doesNotThrowAnyException();
    }

    @Test
    void pagesPaintAtAnySize() {
        for (Dimension size : new Dimension[] {
            new Dimension(0, 0), new Dimension(1, 1), new Dimension(300, 400), new Dimension(1024, 768)
        }) {
            assertThatCode(() -> paint(new ScreenPainter(new WaitingPage(), true, "1.1"), WAITING, size))
                    .doesNotThrowAnyException();
            assertThatCode(() -> paint(new ScreenPainter(new DebugPage(), false, "1.1"), LIVE, size))
                    .doesNotThrowAnyException();
        }
    }
}
