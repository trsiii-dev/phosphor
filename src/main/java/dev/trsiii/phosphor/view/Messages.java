package dev.trsiii.phosphor.view;

/** Every string a page shows. Kept in one place so the wording stays consistent. */
public final class Messages {

    public static final String WAITING_TITLE = "Waiting for Star Citizen.";
    public static final String STREAM_NOT_ANSWERING = "The telemetry stream is not answering.";
    public static final String PAGES_APPEAR = "The pages appear by themselves.";
    public static final String THANKS = "Thank you for using Phosphor.";
    public static final String PAGE_ERROR = "page error";

    public static final String LIVE = "LIVE";
    public static final String NO_STREAM = "NO STREAM";
    public static final String DATA = "DATA";

    public static final String FLIGHT_MODE = "FLIGHT MODE";
    public static final String LANDING_GEAR = "LANDING GEAR";
    public static final String GEAR_DOWN = "DOWN";
    public static final String GEAR_UP = "UP";
    public static final String DOORS = "DOORS";
    public static final String DOORS_OPEN = "OPEN";
    public static final String DOORS_CLOSED = "CLOSED";

    public static final String WARNING_LIGHTS = "WARNING LIGHTS";
    public static final String LIT = " LIT";
    public static final String NOMINAL = "NOMINAL";
    public static final String MINOR = "MINOR";
    public static final String MAJOR = "MAJOR";

    public static final String PILOT = "PILOT";
    public static final String AIR = "AIR";
    public static final String BREATHABLE = "BREATHABLE";
    public static final String NOT_BREATHABLE = "NOT BREATHABLE";

    /** Shown in place of a value the game did not send. Never a zero. */
    public static final String UNKNOWN = "---";

    private Messages() {}

    /** Footer line, for example "DEFAULT · PHOSPHOR 1.1" on a screen's default page. */
    public static String footer(String build, boolean defaultPage) {
        return (defaultPage ? "DEFAULT · " : "") + "PHOSPHOR " + build;
    }
}
