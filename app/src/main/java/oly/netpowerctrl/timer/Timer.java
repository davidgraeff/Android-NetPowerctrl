package oly.netpowerctrl.timer;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.StorableInterface;
import oly.netpowerctrl.device_ports.DevicePort;

/**
 * Created by david on 19.05.14.
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

        writer.close();
    }

    @Override
    public StorableDataType getDataType() {
        return StorableDataType.JSON;
    }

    @Override
    public String getStorableName() {
        return String.valueOf(id);
    }

    @Override
    public void load(JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        Timer timer = this;
        timer.fromCache = true;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "id":
                    timer.id = reader.nextLong();
                    break;
                case "port_id":
                    timer.port_id = UUID.fromString(reader.nextString());
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

        reader.endObject();

        if (timer.port_id == null)
            throw new ClassNotFoundException();

        timer.port = AppData.getInstance().findDevicePort(timer.port_id);
        if (timer.port == null)
            throw new ClassNotFoundException();
    }

    @Override
    public void load(InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }
}
