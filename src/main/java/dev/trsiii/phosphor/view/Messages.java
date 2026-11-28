package dev.trsiii.phosphor.view;

/** Every string a page shows. Kept in one place so the wording stays consistent. */
public final class Messages {

    public static final String WAITING_TITLE = "Waiting for Star Citizen.";
    public static final String STREAM_NOT_ANSWERING = "The telemetry stream is not answering.";
    public static final String PAGES_APPEAR = "The pages appear by themselves.";
    public static final String THANKS = "Thank you for using Phosphor.";
    public static final String PAGE_ERROR = "page error";

    /** Shown in place of a value the game did not send. Never a zero. */
    public static final String UNKNOWN = "---";

    private Messages() {}

    /** Footer line, for example "DEFAULT · PHOSPHOR 1.1" on a screen's default page. */
    public static String footer(String build, boolean defaultPage) {
        return (defaultPage ? "DEFAULT · " : "") + "PHOSPHOR " + build;
    }
}
