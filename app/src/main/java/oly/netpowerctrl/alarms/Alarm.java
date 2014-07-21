package oly.netpowerctrl.alarms;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePort;

/**
 * Created by david on 19.05.14.
 */
public class Alarm {
    // Alarm type
    public static final int TYPE_RANGE_ON_WEEKDAYS = 1;
    public int type = TYPE_RANGE_ON_WEEKDAYS;
    public static final int TYPE_RANGE_ON_RANDOM_WEEKDAYS = 2;
    public static final int TYPE_ONCE = 10; // fixed date+time
    public static final int TYPES = 3;
    // Unique ID
    public long id = -1;
    public UUID port_id;
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
    public boolean enabled = false;
    // If this alarm is not from device (yet) but only from the cache, this flag is set.
    public boolean fromCache = false;
    // Store days. Start with SUNDAY
    public boolean weekdays[] = new boolean[7];
    // Absolute date and time
    public Date absolute_date;

    // Relative alarm in minutes of the day: hour*60+minute. -1 for disabled
    public int hour_minute_start = -1;
    // For ranged alarms in minutes of the day: hour*60+minute
    public int hour_minute_stop = -1;
    // For random alarms in minutes of the day: hour*60+minute
    public int hour_minute_random_interval = -1;

    public static Alarm fromJSON(JsonReader reader) throws IOException, ClassNotFoundException, ParseException {
        reader.beginObject();
        Alarm di = new Alarm();
        di.fromCache = true;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "id":
                    di.id = reader.nextLong();
                    break;
                case "port_id":
                    di.port_id = UUID.fromString(reader.nextString());
                    break;
                case "deviceAlarm":
                    di.deviceAlarm = reader.nextBoolean();
                    break;
                case "freeDeviceAlarm":
                    di.freeDeviceAlarm = reader.nextBoolean();
                    break;
                case "enabled":
                    di.enabled = reader.nextBoolean();
                    break;
                case "type":
                    di.type = reader.nextInt();
                    break;
                case "absolute_date":
                    di.absolute_date = DateFormat.getDateInstance().parse(reader.nextString());
                    break;
                case "hour_minute_start":
                    di.hour_minute_start = reader.nextInt();
                    break;
                case "hour_minute_stop":
                    di.hour_minute_stop = reader.nextInt();
                    break;
                case "hour_minute_random_interval":
                    di.hour_minute_random_interval = reader.nextInt();
                    break;
                case "weekdays":
                    reader.beginArray();
                    int i = 0;
                    while (reader.hasNext()) {
                        di.weekdays[i++] = reader.nextBoolean();
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (di.port_id == null)
            throw new ClassNotFoundException();

        di.port = NetpowerctrlApplication.getDataController().findDevicePort(di.port_id);
        if (di.port == null)
            throw new ClassNotFoundException();

        return di;
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

    /**
     * Convert minutes of the day to a string representation like "11:12".
     *
     * @param hour_minute minutes of the day: hour*60+minute
     * @return A 24 hour based string representation.
     */
    public String time(int hour_minute) {
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

    public String getTargetName() {
        if (port != null)
            return port.device.DeviceName + ": " + port.getDescription();
        else
            return "";
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(id);
        writer.name("port_id").value(port_id.toString());
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
    }

}
