package oly.netpowerctrl.plugin_anel;

/**
 * Created by david on 05.12.14.
 */
public class AnelTimer {

    // Store days. Start with SUNDAY
    public final boolean[] weekdays = new boolean[7];

    // Relative alarm in minutes of the day: hour*60+minute. -1 for disabled
    public int hour_minute_start = -1;
    public int hour_minute_stop = -1;
    public int hour_minute_random_interval = -1;

    /**
     * Convert minutes of the day to a string representation like "11:12".
     *
     * @param hour_minute minutes of the day: hour*60+minute
     * @return A 24 hour based string representation.
     */
    public static String time(int hour_minute) {
        if (hour_minute == -1)
            return "-";
        int hour = hour_minute / 60;
        int minute = hour_minute % 60;
        return (hour < 9 ? "0" : "") + String.valueOf(hour) + ":" + (minute < 9 ? "0" : "") + String.valueOf(minute);
    }
}
