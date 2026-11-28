package dev.trsiii.phosphor.view;

/**
 * A value ready to draw. The number and its unit label come from the same call,
 * so a page cannot pair a converted number with the wrong label.
 *
 * @param text the number, or "---" if it is unknown
 * @param unit the unit label, or an empty string if the value has none
 */
public record Reading(String text, String unit) {

    /** Text and unit as one string, for example "37 C". */
    public String joined() {
        return unit.isEmpty() ? text : text + " " + unit;
    }
}
