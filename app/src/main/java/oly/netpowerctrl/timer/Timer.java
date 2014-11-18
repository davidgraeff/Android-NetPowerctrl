package oly.netpowerctrl.timer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.main.App;

/**
 * Represents an alarm and is used by TimerCollection.
 */
public class Timer implements StorableInterface {
    // Alarm type
    public static final int TYPE_RANGE_ON_WEEKDAYS = 1;
    public int type = TYPE_RANGE_ON_WEEKDAYS;
    public static final int TYPE_RANGE_ON_RANDOM_WEEKDAYS = 2;
    public static final int TYPE_ONCE = 10; // fixed date+time
    public static final int TYPES = 3;
    // Store days. Start with SUNDAY
    public final boolean[] weekdays = new boolean[7];
    // Unique ID
    public long id = -1;
    public String executable_uid;
    // Temporary
    public Executable executable;
    /**
     * True if the alarm is from a plugin/device and not a virtual alarm on the android system
     */
    public boolean deviceAlarm = true;
    /**
     * Not armed device alarm that is available to be programmed. It will not be shown on the
     * alarm list. To distinguish between a free alarm slot and a programmed but "disabled" alarm,
     * "freeDeviceAlarm" is only set, if the alarm values are the default values. For anel devices
     * this is: All weekdays, on-time from 00:00 to 23:59.
     */
    public boolean freeDeviceAlarm;
    // True if the alarm is enabled.
    public boolean enabled = true;
    // If this alarm is not from device (yet) but only from the cache, this flag is set.
    public boolean fromCache = false;
    // Absolute date and time
    public Date absolute_date;

    // Relative alarm in minutes of the day: hour*60+minute. -1 for disabled
    public int hour_minute_start = -1;
    // For ranged alarms in minutes of the day: hour*60+minute
    public int hour_minute_stop = -1;
    // For random alarms in minutes of the day: hour*60+minute
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

    public static int getHour(int hour_minute) {
        return hour_minute / 60;
    }

    public static int getMinute(int hour_minute) {
        return hour_minute % 60;
    }

    /**
     * Return a localised string of all active weekdays.
     *
     * @return A string like this: "Mo, Di, Mi" if monday, tuesday and wednesdays is active
     * on a german system.
     */
    public String days() {
        String d = "";
        // Get all weekdays in the short variant localised. The first entry is the empty string.
        String[] weekDays_Strings = DateFormatSymbols.getInstance().getShortWeekdays();

        boolean anyActive = false;
        // Add all active days
        for (int i = 0; i < 7; ++i)
            if (weekdays[i]) {
                anyActive = true;
                d += weekDays_Strings[i + 1] + ", ";
            }

        // Cut off the last ", ".
        if (anyActive)
            d = d.substring(0, d.length() - 3);
        return d;
    }

    public String toString(Context context) {
        String pre = "";
        if (executable instanceof DevicePort)
            pre = ((DevicePort) executable).device.getDeviceName() + ": " + executable.getTitle() + " - ";

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

    public String getTargetName() {
        if (executable != null)
            return executable.getDescription(App.instance) + ": " + executable.getTitle();
        return "";
    }

    /**
     * Return the json representation of this alarm
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    private void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(id);
        writer.name("executable_uid").value(executable_uid);
        writer.name("deviceAlarm").value(deviceAlarm);
        writer.name("freeDeviceAlarm").value(freeDeviceAlarm);
        writer.name("enabled").value(enabled);
        writer.name("type").value(type);
        if (absolute_date != null)
            writer.name("absolute_date").value(DateFormat.getDateInstance().format(absolute_date));
        writer.name("hour_minute_start").value(hour_minute_start);
        writer.name("hour_minute_stop").value(hour_minute_stop);
        writer.name("hour_minute_random_interval").value(hour_minute_random_interval);

        writer.name("weekdays").beginArray();
        for (boolean b : weekdays) {
            writer.value(b);
        }
        writer.endArray();

        writer.endObject();

        writer.close();
    }

    @Override
    public String getStorableName() {
        return String.valueOf(id);
    }

    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        Timer timer = this;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "id":
                    timer.id = reader.nextLong();
                    break;
                case "executable_uid":
                    timer.executable_uid = reader.nextString();
                    break;
                case "deviceAlarm":
                    timer.deviceAlarm = reader.nextBoolean();
                    break;
                case "freeDeviceAlarm":
                    timer.freeDeviceAlarm = reader.nextBoolean();
                    break;
                case "enabled":
                    timer.enabled = reader.nextBoolean();
                    break;
                case "type":
                    timer.type = reader.nextInt();
                    break;
                case "absolute_date":
                    try {
                        timer.absolute_date = DateFormat.getDateInstance().parse(reader.nextString());
                    } catch (ParseException e) {
                        throw new IOException(e);
                    }
                    break;
                case "hour_minute_start":
                    timer.hour_minute_start = reader.nextInt();
                    break;
                case "hour_minute_stop":
                    timer.hour_minute_stop = reader.nextInt();
                    break;
                case "hour_minute_random_interval":
                    timer.hour_minute_random_interval = reader.nextInt();
                    break;
                case "weekdays":
                    reader.beginArray();
                    int i = 0;
                    while (reader.hasNext()) {
                        timer.weekdays[i++] = reader.nextBoolean();
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        timer.fromCache = timer.deviceAlarm;

        reader.endObject();

        if (timer.executable_uid == null)
            throw new ClassNotFoundException();

        timer.executable = AppData.getInstance().findExecutable(timer.executable_uid);
        if (timer.executable == null)
            throw new ClassNotFoundException(timer.toString());
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    public void markFromCache() {
        if (deviceAlarm)
            fromCache = true;
    }

    public NextAlarm getNextAlarmUnixTime(long currentTime) {
        NextAlarm nextAlarm = new NextAlarm();
        Calendar calendar_start = Calendar.getInstance();
        Calendar calendar_stop = Calendar.getInstance();
        calendar_start.setTimeInMillis(currentTime);
        calendar_stop.setTimeInMillis(currentTime);
        int day = calendar_start.get(Calendar.DAY_OF_WEEK) - 1; // start with 1: Sunday
        nextAlarm.command = DevicePort.TOGGLE;
        nextAlarm.timerId = id;

        Calendar calendar = null;

        if (hour_minute_start != -1) {
            calendar_start.set(Calendar.HOUR, getHour(hour_minute_start));
            calendar_start.set(Calendar.MINUTE, getMinute(hour_minute_start));
            for (int additionalDays = 0; additionalDays < 8; ++additionalDays) {
                if (weekdays[(additionalDays + day) % 7]) {
                    if (calendar_start.before(Calendar.getInstance())) {
                        calendar_start.add(Calendar.HOUR, 24);
                        continue;
                    }
                    calendar = calendar_start;
                    nextAlarm.command = DevicePort.ON;
                    break;
                } else
                    calendar_start.add(Calendar.HOUR, 24);
            }
        }

        if (hour_minute_stop != -1) {
            calendar_stop.set(Calendar.HOUR, getHour(hour_minute_stop));
            calendar_stop.set(Calendar.MINUTE, getMinute(hour_minute_stop));
            for (int additionalDays = 0; additionalDays < 8; ++additionalDays) {
                if (weekdays[(additionalDays + day) % 7]) {
                    if (calendar_stop.before(Calendar.getInstance())) {
                        calendar_stop.add(Calendar.HOUR, 24);
                        continue;
                    }
                    if (calendar == null || calendar.after(calendar_stop)) {
                        calendar = calendar_stop;
                        nextAlarm.command = DevicePort.OFF;
                    }
                    break;
                } else
                    calendar_stop.add(Calendar.HOUR, 24);
            }
        }

        nextAlarm.unix_time = calendar != null ? calendar.getTimeInMillis() : 0;
        return nextAlarm;
    }

    public class NextAlarm {
        public long unix_time;
        public int command;
        public long timerId;
    }
}
