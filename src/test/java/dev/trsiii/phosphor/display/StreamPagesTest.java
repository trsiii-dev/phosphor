package dev.trsiii.phosphor.display;

import static org.assertj.core.api.Assertions.assertThat;

import dev.trsiii.phosphor.config.UnitSystem;
import dev.trsiii.phosphor.pages.FlightPage;
import dev.trsiii.phosphor.pages.LightsPage;
import dev.trsiii.phosphor.pages.Look;
import dev.trsiii.phosphor.pages.MfdPage;
import dev.trsiii.phosphor.pages.VitalsPage;
import dev.trsiii.phosphor.pages.WaitingPage;
import dev.trsiii.phosphor.source.DemoSource;
import dev.trsiii.phosphor.telemetry.Telemetry;
import dev.trsiii.phosphor.tracker.Snapshot;
import dev.trsiii.phosphor.tracker.StreamPhase;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The three pages that read the stream, painted into an image in each state they can be in. */
class StreamPagesTest {

    // One design unit is one pixel at this size.
    private static final Dimension SIZE = new Dimension(600, 800);

    private static final Instant MESSAGE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Telemetry SAMPLE = DemoSource.sample(0);

    private static final List<MfdPage> PAGES = List.of(new LightsPage(), new FlightPage(), new VitalsPage());

    private static Snapshot live(Telemetry telemetry, UnitSystem units) {
        return Snapshot.of(new StreamPhase.Live(MESSAGE), MESSAGE.plusMillis(50), telemetry, units);
    }

    private static Snapshot stale(Telemetry telemetry) {
        return Snapshot.of(new StreamPhase.Stale(MESSAGE), MESSAGE.plusSeconds(3), telemetry, UnitSystem.METRIC);
    }

    private static int[] paint(MfdPage page, boolean defaultPage, Snapshot snapshot) {
        BufferedImage image = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ScreenPainter(page, defaultPage, "1.1").paint(g, snapshot, SIZE);
        } finally {
            g.dispose();
        }
        return image.getRGB(0, 0, SIZE.width, SIZE.height, null, 0, SIZE.width);
    }

    private static int pixel(int[] image, int x, int y) {
        return image[y * SIZE.width + x];
    }

    /** Number of pixels between two rows that are not the background color. */
    private static long inked(int[] image, int fromRow, int toRow) {
        long count = 0;
        for (int i = fromRow * SIZE.width; i < toRow * SIZE.width; i++) {
            if (image[i] != Look.GROUND.getRGB()) {
                count++;
            }
        }
        return count;
    }

    @Test
    void everyPagePaintsInEveryUnitsSetting() {
        for (MfdPage page : PAGES) {
            for (UnitSystem units : UnitSystem.values()) {
                int[] image = paint(page, true, live(SAMPLE, units));

                assertThat(inked(image, 40, 100)).as("header of %s", page.name()).isPositive();
                assertThat(inked(image, 100, 740)).as("cells of %s", page.name()).isPositive();
                assertThat(inked(image, 765, 795)).as("footer of %s", page.name()).isPositive();
            }
        }
    }

    @Test
    void aPageWithNoValuesStillPaints() {
        for (MfdPage page : PAGES) {
            int[] image = paint(page, true, live(Telemetry.EMPTY, UnitSystem.METRIC));

            assertThat(inked(image, 100, 740)).as("cells of %s", page.name()).isPositive();
        }
    }

    @Test
    void staleValuesLookDifferentFromLiveOnes() {
        for (MfdPage page : PAGES) {
            assertThat(paint(page, true, stale(SAMPLE)))
                    .as(page.name())
                    .isNotEqualTo(paint(page, true, live(SAMPLE, UnitSystem.METRIC)));
        }
    }

    @Test
    void withoutTheStreamEveryPageGivesWayToTheWaitingPage() {
        Snapshot absent = Snapshot.of(new StreamPhase.Waiting(), MESSAGE);
        // The Waiting page is not the screen's default page, so its footer does not say DEFAULT.
        int[] waiting = paint(new WaitingPage(), false, absent);

        for (MfdPage page : PAGES) {
            assertThat(paint(page, true, absent)).as(page.name()).isEqualTo(waiting);
        }
    }

    @Test
    void aMajorWarningFillsItsTile() {
        // A point inside the HIT tile, clear of its text.
        int x = 12 + 250;
        int y = 558 + 95;

        assertThat(pixel(paint(new LightsPage(), true, live(SAMPLE, UnitSystem.METRIC)), x, y))
                .isEqualTo(Look.ALERT.getRGB());
        // A stale warning must not look current.
        assertThat(pixel(paint(new LightsPage(), true, stale(SAMPLE)), x, y)).isEqualTo(Look.STALE.getRGB());
        // A light with no known state is never drawn as a warning.
        assertThat(pixel(paint(new LightsPage(), true, live(Telemetry.EMPTY, UnitSystem.METRIC)), x, y))
                .isEqualTo(Look.GROUND.getRGB());
    }
}
