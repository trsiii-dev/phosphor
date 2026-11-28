package dev.trsiii.phosphor;

import java.nio.file.Path;
import java.util.Optional;

/** Parsed command-line arguments. */
record CommandLine(Optional<Path> config, boolean windowed, boolean demo, boolean help, boolean version) {

    static final String USAGE = """
            Usage: phosphor [--config <file>] [--windowed] [--demo] [--version] [--help]

              --config <file>   read this config file in place of the usual one
              --windowed        show the pages in ordinary desktop windows
              --demo            windowed, with made-up data in place of the game
              --version         print the version and exit
              --help            print this text and exit
            """;

    /** Throws IllegalArgumentException with a user-facing message on a bad argument. */
    static CommandLine parse(String... args) {
        Optional<Path> config = Optional.empty();
        boolean windowed = false;
        boolean demo = false;
        boolean help = false;
        boolean version = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help") || arg.equals("-h")) {
                help = true;
            } else if (arg.equals("--windowed")) {
                windowed = true;
            } else if (arg.equals("--demo")) {
                demo = true;
            } else if (arg.equals("--version")) {
                version = true;
            } else if (arg.equals("--config")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("--config needs a file name after it.");
                }
                i++;
                config = Optional.of(Path.of(args[i]));
            } else if (arg.startsWith("--config=")) {
                config = Optional.of(Path.of(arg.substring("--config=".length())));
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        return new CommandLine(config, windowed, demo, help, version);
    }
}
