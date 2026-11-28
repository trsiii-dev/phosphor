package dev.trsiii.phosphor.display;

import dev.trsiii.phosphor.config.AppConfig;
import dev.trsiii.phosphor.config.ScreenConfig;
import dev.trsiii.phosphor.pages.DebugPage;
import dev.trsiii.phosphor.pages.MfdPage;
import dev.trsiii.phosphor.pages.PageRegistry;
import dev.trsiii.phosphor.pages.WaitingPage;
import dev.trsiii.phosphor.tracker.Snapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * Windowed mode: every configured screen's page in an ordinary desktop window,
 * plus a debug window. For development, so the pages can be worked on without
 * the MFD screens.
 */
public final class DesktopWindows {

    private DesktopWindows() {}

    /** Must be called on the event dispatch thread. Closing any window ends the program. */
    public static void open(AppConfig config, PageRegistry pages, String build, Supplier<Snapshot> snapshots) {
        List<PageHost> hosts = new ArrayList<>();
        for (ScreenConfig screen : config.screens()) {
            Optional<MfdPage> page = pages.find(screen.page());
            // A page that is not built yet shows the Waiting page, and the footer does not call it the default.
            ScreenPainter painter =
                    new ScreenPainter(page.orElseGet(WaitingPage::new), page.isPresent(), build);
            hosts.add(show(screen.name() + " " + screen.page(), painter, snapshots));
        }
        if (hosts.isEmpty()) {
            hosts.add(show("WAITING", new ScreenPainter(new WaitingPage(), false, build), snapshots));
        }
        hosts.add(show("DEBUG", new ScreenPainter(new DebugPage(), false, build), snapshots));

        // Swing timers fire on the event dispatch thread, so refresh() needs no locking.
        Timer timer = new Timer(1000 / config.display().frameRateCap(), event -> hosts.forEach(PageHost::refresh));
        timer.start();
    }

    private static PageHost show(String title, ScreenPainter painter, Supplier<Snapshot> snapshots) {
        PageHost host = new PageHost(painter, snapshots);
        JFrame frame = new JFrame("Phosphor: " + title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(host);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        return host;
    }
}
