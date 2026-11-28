package dev.trsiii.phosphor.config;

import java.time.Duration;

/**
 * Telemetry stream settings.
 *
 * @param enabled           false disables the stream entirely
 * @param address           stream host, which must be this machine
 * @param port              stream port (the game uses 2942)
 * @param connectTimeout    timeout for a single connection attempt
 * @param reconnectInterval delay between connection attempts while the stream is absent
 * @param staleAfter        values older than this are shown as stale
 * @param giveUpAfter       time without a message before a page shows no data
 */
public record StreamConfig(
        boolean enabled,
        String address,
        int port,
        Duration connectTimeout,
        Duration reconnectInterval,
        Duration staleAfter,
        Duration giveUpAfter) {}
