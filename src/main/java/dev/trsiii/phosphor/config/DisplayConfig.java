package dev.trsiii.phosphor.config;

/**
 * Display settings.
 *
 * @param frameRateCap maximum repaints per second
 * @param windowed     use ordinary desktop windows, for development
 * @param textScale    text size multiplier, where 1.0 is the design size
 */
public record DisplayConfig(int frameRateCap, boolean windowed, double textScale) {}
