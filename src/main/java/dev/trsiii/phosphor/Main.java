package dev.trsiii.phosphor;

import dev.trsiii.phosphor.config.AppConfig;
import dev.trsiii.phosphor.config.ConfigException;
import dev.trsiii.phosphor.config.ConfigLoader;
import dev.trsiii.phosphor.config.ConfigLocation;
import dev.trsiii.phosphor.config.StartupValidator;
import dev.trsiii.phosphor.config.StreamConfig;
import dev.trsiii.phosphor.display.DesktopWindows;
import dev.trsiii.phosphor.pages.PageRegistry;
import dev.trsiii.phosphor.source.DemoSource;
import dev.trsiii.phosphor.tracker.MonotonicClock;
import dev.trsiii.phosphor.tracker.StreamMachine;
import dev.trsiii.phosphor.tracker.Ticker;
import dev.trsiii.phosphor.tracker.Tracker;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

/**
 * Entry point. Loads and validates the config and logs a startup line. In
 * windowed mode it then starts the tracker and opens the pages in desktop windows.
 */
public final class Main {

    private static final System.Logger LOG = System.getLogger(Main.class.getName());

    /** Valid page names. TODO: replace with the page registry. */
    static final List<String> KNOWN_PAGES = List.of("LIGHTS", "FLIGHT", "VITALS");

    private static final int OK = 0;
    private static final int CANNOT_START = 2;
    // Not an exit code. Windows are open and the program keeps running.
    private static final int RUNNING = -1;

    private Main() {}

    public static void main(String[] args) {
        int code = run(args);
        if (code != RUNNING) {
            System.exit(code);
        }
    }

    /** Returns the process exit code, or RUNNING if windows were opened. */
    static int run(String[] args) {
        CommandLine commandLine;
        try {
            commandLine = CommandLine.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.print(CommandLine.USAGE);
            return CANNOT_START;
        }
        if (commandLine.help()) {
            System.out.print(CommandLine.USAGE);
            return OK;
        }
        if (commandLine.version()) {
            System.out.println("Phosphor " + BuildInfo.version());
            return OK;
        }

        Logging.setUp(ConfigLocation.logDir());

        Path file = commandLine.config().orElseGet(ConfigLocation::configFile).toAbsolutePath();
        AppConfig config;
        try {
            if (!Files.isRegularFile(file)) {
                throw new ConfigException("There is no such file. Copy run/config.example.yaml there and"
                        + " edit it, or name another file with --config.");
            }
            config = ConfigLoader.load(file);
            StartupValidator.validate(config, KNOWN_PAGES);
        } catch (ConfigException e) {
            LOG.log(System.Logger.Level.ERROR, "Phosphor cannot start. Config file " + file + ":"
                    + System.lineSeparator() + e.getMessage());
            return CANNOT_START;
        }

        LOG.log(System.Logger.Level.INFO, startupLine(BuildInfo.version(), config));

        if (!commandLine.windowed() && !commandLine.demo() && !config.display().windowed()) {
            // TODO: open the MFD windows. Until then only windowed mode shows anything.
            return OK;
        }
        if (GraphicsEnvironment.isHeadless()) {
            LOG.log(System.Logger.Level.ERROR, "Phosphor cannot open windows: no display is available.");
            return CANNOT_START;
        }
        Clock clock = new MonotonicClock();
        StreamConfig stream = config.stream();
        Tracker tracker = new Tracker(
                new StreamMachine(stream.staleAfter(), stream.giveUpAfter()),
                config.units(),
                clock.instant(),
                Main::quitWithTheGame);
        tracker.start();
        new Ticker(clock, tracker::post).start();
        if (commandLine.demo()) {
            new DemoSource(clock).start(tracker::post);
        }
        // TODO: start the stream client here once it exists.

        AppConfig loaded = config;
        SwingUtilities.invokeLater(
                () -> DesktopWindows.open(loaded, PageRegistry.standard(), BuildInfo.version(), tracker::snapshot));
        return RUNNING;
    }

    // Runs on the tracker thread once the stream has been gone for good.
    private static void quitWithTheGame() {
        LOG.log(System.Logger.Level.INFO, "The game has closed. Phosphor is quitting.");
        System.exit(OK);
    }

    static String startupLine(String version, AppConfig config) {
        StreamConfig stream = config.stream();
        String streamPart = stream.enabled() ? "stream " + stream.address() + ":" + stream.port() : "stream off";
        String screensPart = config.screens().isEmpty()
                ? "no screens"
                : "screens " + config.screens().stream()
                        .map(screen -> screen.name() + " " + screen.page())
                        .collect(Collectors.joining("; "));
        return "Phosphor " + version + " starting: units " + config.units().configName() + ", " + streamPart
                + ", " + screensPart;
    }
}
