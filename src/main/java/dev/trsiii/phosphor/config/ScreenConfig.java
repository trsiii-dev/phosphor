package dev.trsiii.phosphor.config;

/**
 * One output screen. A screen is identified by its position and size in the
 * desktop layout. The OS display name is not used because it can change when a
 * cable is replugged.
 *
 * @param name     user-chosen label, used in messages
 * @param x        left edge in the desktop layout, in pixels
 * @param y        top edge, in pixels
 * @param width    width in pixels
 * @param height   height in pixels
 * @param page     initial page name, uppercase
 * @param rotation rotation of the drawing (0, 90, 180, or 270)
 */
public record ScreenConfig(String name, int x, int y, int width, int height, String page, int rotation) {}
