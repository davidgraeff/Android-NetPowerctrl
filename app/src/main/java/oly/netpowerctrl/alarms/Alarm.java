package oly.netpowerctrl.alarms;

import java.util.Date;

/**
 * Created by david on 19.05.14.
 */
public class Alarm {
    // Unique ID
    long id;

    // Anel devices
    static final int TYPE_RANGE_ON_WEEKDAYS = 1;
    static final int TYPE_RANGE_ON_RANDOM_WEEKDAYS = 2;
    // Generic alarms
    static final int TYPE_ONCE = 10;
    static final int TYPE_REPEAT = 11;

    // Store days
    public boolean weekdays[] = new boolean[7];
    // Absolute date and time
    public Date absolute_date;
    // Relative alarm in minutes of the day: hour*60+minute
    public int hour_minute_start;
    // For ranged alarms in minutes of the day: hour*60+minute
    public int hour_minute_stop;
    // For random alarms in minutes of the day: hour*60+minute
    public int hour_minute_random_interval;
}
