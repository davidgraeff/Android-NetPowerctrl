package oly.netpowerctrl.alarms;

import android.content.Context;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Date;

import oly.netpowerctrl.R;
import oly.netpowerctrl.devices.DevicePort;

/**
 * Created by david on 19.05.14.
 */
public class Alarm {
    // Unique ID
    public long id;
    public String unique_device_id;

    // Temporary
    public DevicePort port;

    /**
     * True if the alarm is from a plugin/device and not a virtual alarm on the android system
     */
    public boolean deviceAlarm;

    /**
     * Not armed device alarm that is available to be programmed. It will not be shown on the
     * alarm list. To distinguish between a free alarm slot and a programmed but "disabled" alarm,
     * "freeDeviceAlarm" is only set, if the alarm values are the default values. For anel devices
     * this is: All weekdays, on-time from 00:00 to 23:59.
     */
    public boolean freeDeviceAlarm;

    // True if the alarm is enabled.
    public boolean enabled;

    // Alarm type
    public static final int TYPE_RANGE_ON_WEEKDAYS = 1;
    public static final int TYPE_RANGE_ON_RANDOM_WEEKDAYS = 2;
    public static final int TYPE_ONCE = 10; // fixed date+time

    public int type;

    // Store days. Start with SUNDAY
    public boolean weekdays[] = new boolean[7];
    // Absolute date and time
    public Date absolute_date;

    // Relative alarm in minutes of the day: hour*60+minute. -1 for disabled
    public int hour_minute_start;
    // For ranged alarms in minutes of the day: hour*60+minute
    public int hour_minute_stop;
    // For random alarms in minutes of the day: hour*60+minute
    public int hour_minute_random_interval;

    public Alarm(boolean deviceAlarm) {
        this.deviceAlarm = deviceAlarm;
    }

    private String days() {
        String d = "";
        // The first entry is the empty string
        String[] weekDays_Strings = DateFormatSymbols.getInstance().getShortWeekdays();

        for (int i = 0; i < 7; ++i)
            if (weekdays[i])
                d += weekDays_Strings[i + 1] + ", ";
        return d;
    }

    private String time(int hour_minute) {
        if (hour_minute == -1)
            return "-";
        int hour = hour_minute / 60;
        int minute = hour_minute % 60;
        return (hour < 9 ? "0" : "") + String.valueOf(hour) + ":" + (minute < 9 ? "0" : "") + String.valueOf(minute);
    }

    public String toString(Context context) {
        String pre = "";
        if (port != null)
            pre = port.device.DeviceName + ": " + port.getDescription() + " - ";

        switch (type) {
            case TYPE_RANGE_ON_WEEKDAYS:
                return pre + context.getString(R.string.alarm_weekdays_range,
                        days(), time(hour_minute_start), time(hour_minute_stop));
            case TYPE_RANGE_ON_RANDOM_WEEKDAYS:
                return pre + context.getString(R.string.alarm_weekdays_range_alarm,
                        days(), time(hour_minute_start), time(hour_minute_stop), time(hour_minute_random_interval));
            case TYPE_ONCE:
                return pre + context.getString(R.string.alarm_once, DateFormat.getInstance().format(absolute_date));
        }
        return pre;
    }
}
