package dev.trsiii.phosphor;

import dev.trsiii.phosphor.config.AppConfig;
import dev.trsiii.phosphor.config.ConfigException;
import dev.trsiii.phosphor.config.ConfigLoader;
import dev.trsiii.phosphor.config.ConfigLocation;
import dev.trsiii.phosphor.config.StartupValidator;
import dev.trsiii.phosphor.config.StreamConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Entry point. For now it loads and validates the config, logs a startup line, and exits. */
public final class Main {

    private static final System.Logger LOG = System.getLogger(Main.class.getName());

    /** Valid page names. TODO: replace with the page registry. */
    static final List<String> KNOWN_PAGES = List.of("LIGHTS", "FLIGHT", "VITALS");

    private static final int OK = 0;
    private static final int CANNOT_START = 2;

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Returns the process exit code. */
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
        return OK;
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
