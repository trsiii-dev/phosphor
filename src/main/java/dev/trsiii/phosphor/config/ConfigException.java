package dev.trsiii.phosphor.config;

/**
 * Thrown for a bad config file. The message is user-facing. For a bad entry it
 * starts with the key, such as {@code stream.port}.
 */
public final class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }
}
