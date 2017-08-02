package com.ciphertechsolutions.io.applicationLogic;

import java.time.Duration;

/**
 *  A class to contain some generic useful helper functions for ION.
 */
public class Utils {

    /**
     * Converts the given duration to a string in the format of "x days hh:mm:ss", "hh:mm:ss", or "x seconds",
     * depending on the amount of time in the duration.
     * @param duration The duration to convert to a string.
     * @return The duration as a string.
     */
    public static String getPrettyTime(Duration duration) {
        StringBuilder timeString = new StringBuilder();
        long seconds = duration.getSeconds();
        addDays(duration, timeString);
        addHours(duration, timeString, seconds);
        addMinutesAndSeconds(duration, timeString, seconds);
        return timeString.toString();
    }

    private static void addMinutesAndSeconds(Duration duration, StringBuilder timeString, long seconds) {
        if (duration.toMinutes() >= 1) {
            int remainingMinutes = (int) ((seconds % 3600) / 60);
            padIfNeeded(timeString, remainingMinutes);
            timeString.append(":");
            int remainingSeconds = (int) (seconds % 60);
            padIfNeeded(timeString, remainingSeconds);
        }
        else {
            timeString.append(seconds % 60);
            timeString.append(" seconds.");
        }
    }

    private static void addHours(Duration duration, StringBuilder timeString, long seconds) {
        if (duration.toHours() >= 1) {
            timeString.append((seconds % 86400)/ 3600);
            timeString.append(":");
        }
    }

    private static void addDays(Duration duration, StringBuilder timeString) {
        if (duration.toDays() >= 1) {
            timeString.append(duration.toDays());
            timeString.append(" days ");
        }
    }

    private static void padIfNeeded(StringBuilder timeString, int remaining) {
        if (remaining < 10) {
            timeString.append("0");
        }
        timeString.append(remaining);
    }
}
