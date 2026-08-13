package dev.vaadin.talk.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared display formatting for talk data, so the public listing and the admin
 * grid render dates and durations identically.
 */
public final class TalkFormats {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.ENGLISH);

    private TalkFormats() {
    }

    /**
     * Formats a scheduled date for display, for example {@code "Wed 20 Aug 2025, 09:00"}.
     *
     * @param value the date to format, may be {@code null}
     * @return the formatted date, or an empty string if {@code value} is {@code null}
     */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    /**
     * Formats a duration in minutes as a compact human-readable string, for
     * example {@code "45 min"}, {@code "3 h"} or {@code "2 h 30 min"}.
     *
     * @param minutes the duration in minutes, may be {@code null}
     * @return the formatted duration, or an empty string if {@code minutes} is {@code null}
     */
    public static String formatDuration(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return remainder == 0 ? hours + " h" : "%d h %d min".formatted(hours, remainder);
    }
}
