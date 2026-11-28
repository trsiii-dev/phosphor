package dev.trsiii.phosphor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logging setup. Output goes to the console and to rotating files in the per-user
 * log directory. The packaged Windows build has no console, so the files are the
 * only log there.
 *
 * <p>The code logs through {@code System.Logger}, which is backed by
 * {@code java.util.logging}, and this class configures that backend. gRPC also
 * logs through {@code java.util.logging}, so its output will go to the same handlers.
 */
final class Logging {

    private static final int FILE_SIZE = 1_000_000; // bytes per file before rotating
    private static final int FILE_COUNT = 3; // files kept

    private Logging() {}

    static void setUp(Path logDir) {
        // Configure the root logger. Every other logger inherits from it.
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        root.setLevel(Level.INFO);

        Handler console = new ConsoleHandler();
        console.setFormatter(new OneLine());
        root.addHandler(console);

        try {
            Files.createDirectories(logDir);
            // Owner-only where the file system supports it, so other local users
            // cannot read the logs.
            if (logDir.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(logDir, PosixFilePermissions.fromString("rwx------"));
            }
            // %g is the rotation index, and phosphor.0.log is the newest. A literal %
            // in the directory path has to be escaped as %% for FileHandler.
            String pattern = logDir.toString().replace("%", "%%") + File.separator + "phosphor.%g.log";
            Handler file = new FileHandler(pattern, FILE_SIZE, FILE_COUNT, true);
            file.setFormatter(new OneLine());
            file.setEncoding("UTF-8");
            root.addHandler(file);
        } catch (IOException | RuntimeException e) {
            // Never fail startup over the log file.
            System.getLogger(Logging.class.getName())
                    .log(System.Logger.Level.WARNING, "No log file will be written: " + e);
        }
    }

    /** Formats a record as "time LEVEL message", then the stack trace if there is one. */
    private static final class OneLine extends Formatter {

        private static final DateTimeFormatter TIME =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord record) {
            StringBuilder line = new StringBuilder()
                    .append(TIME.format(record.getInstant()))
                    .append(' ')
                    .append(record.getLevel().getName())
                    .append(' ')
                    .append(clean(formatMessage(record)))
                    .append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter trace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(trace, true));
                line.append(trace);
            }
            return line.toString();
        }

        /**
         * Messages can carry text from the config, and later from the stream. This
         * keeps that text from forging a log line or sending escape codes to a
         * terminal. Continuation lines are indented, and other control characters
         * are written as their code point.
         */
        private static String clean(String message) {
            if (message == null) {
                return "";
            }
            StringBuilder out = new StringBuilder(message.length());
            message.codePoints().forEach(cp -> {
                if (cp == '\n') {
                    out.append(System.lineSeparator()).append("    ");
                } else if (cp == '\t') {
                    out.append(' ');
                } else if (cp == '\r') {
                    // Dropped. The line feed that follows starts the new line.
                } else if (Character.isISOControl(cp)
                        || Character.getType(cp) == Character.FORMAT
                        || cp == 0x2028
                        || cp == 0x2029) {
                    out.append(String.format("<U+%04X>", cp));
                } else {
                    out.appendCodePoint(cp);
                }
            });
            return out.toString();
        }
    }
}
