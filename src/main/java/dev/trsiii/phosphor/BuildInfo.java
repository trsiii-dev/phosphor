package dev.trsiii.phosphor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build version, as shown in the page footer ({@code PHOSPHOR 1.1}). It is set in
 * build.gradle.kts, and the build copies it into build.properties.
 */
public final class BuildInfo {

    private static final String VERSION = read();

    private BuildInfo() {}

    public static String version() {
        return VERSION;
    }

    private static String read() {
        // Resolved relative to this class's package.
        try (InputStream in = BuildInfo.class.getResourceAsStream("build.properties")) {
            if (in == null) {
                return "dev";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "dev");
        } catch (IOException e) {
            return "dev";
        }
    }
}
