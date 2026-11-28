package dev.trsiii.phosphor.tracker;

/**
 * Telemetry stream state as seen by a page. It is the only input a page uses to
 * choose between its live, stale, and no-data forms.
 */
public enum StreamState {

    /** No stream. The game is not running or was started without it. */
    ABSENT,

    /** Messages are arriving and the values on screen are current. */
    LIVE,

    /** The stream has ended or gone quiet. The last values are still shown, marked as stale. */
    STALE
}
