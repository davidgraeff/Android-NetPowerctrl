package oly.netpowerctrl.timer;

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
import java.util.UUID;

import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;
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
    private static long NEXT_ID = 0;
    // Store days. Start with SUNDAY
    public final boolean[] weekdays = new boolean[7];
    public final long viewID;
    // Relative alarm in minutes of the day: hour*60+minute. -1 for disabled
    public int hour_minute = -1;
    // Absolute date and time
    public Date absolute_date;
    // Command and executable
    public int command = -100;
    public String executable_uid;
    // Temporary
    public Executable executable;
    public long next_execution_unix_time = 0;
    public AlarmOnDevice alarmOnDevice;
    // True if the alarm is enabled.
    public boolean enabled = true;
    public String uuid;

    /**
     * Do NOT use this constructor! Use createNewTimer instead.
     */
    public Timer() {
        NEXT_ID++;
        viewID = NEXT_ID;
    }

    public static Timer createNewTimer() {
        Timer timer = new Timer();
        timer.uuid = UUID.randomUUID().toString();
        return timer;
    }

    public static Timer createNewOneTimeTimer(Calendar calendar, Executable executable, int command) {
        Timer timer = new Timer();
        timer.uuid = UUID.randomUUID().toString();
        timer.absolute_date = calendar.getTime();
        timer.type = TYPE_ONCE;
        timer.executable = executable;
        timer.executable_uid = executable.getUid();
        timer.command = command;
        timer.computeNextAlarmUnixTime(0);
        return timer;
    }

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

    boolean equals(Timer timer) {
        return uuid.equals(timer.uuid);
    }

    public boolean isFromCache() {
        return alarmOnDevice != null && alarmOnDevice.fromCache;
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
        // DO not write free device alarms to disk.
        if (alarmOnDevice != null && alarmOnDevice.freeDeviceAlarm) {
            writer.close();
            return;
        }
        writer.beginObject();
        writer.name("executable_uid").value(executable_uid);
        if (alarmOnDevice != null) {
            writer.name("alarmOnDevice").beginObject().
                    name("portId").value(alarmOnDevice.portId).
                    name("timerId").value(alarmOnDevice.timerId).
                    endObject();
        }
        writer.name("uniqueID").value(uuid);
        writer.name("enabled").value(enabled);
        writer.name("type").value(type);
        if (absolute_date != null)
            writer.name("absolute_date").value(DateFormat.getDateTimeInstance().format(absolute_date));
        writer.name("hour_minute").value(hour_minute);
        writer.name("command").value(command);

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
        return uuid;
    }

    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "uniqueID":
                    uuid = reader.nextString();
                    break;
                case "executable_uid":
                    executable_uid = reader.nextString();
                    break;
                case "alarmOnDevice":
                    reader.beginObject();
                    alarmOnDevice = new AlarmOnDevice(true);
                    while (reader.hasNext()) {
                        name = reader.nextName();
                        switch (name) {
                            case "portId":
                                alarmOnDevice.portId = (byte) reader.nextInt();
                                break;
                            case "timerId":
                                alarmOnDevice.timerId = (byte) reader.nextInt();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                    break;
                case "enabled":
                    enabled = reader.nextBoolean();
                    break;
                case "type":
                    type = reader.nextInt();
                    break;
                case "absolute_date":
                    try {
                        absolute_date = DateFormat.getDateTimeInstance().parse(reader.nextString());
                    } catch (ParseException e) {
                        throw new IOException(e);
                    }
                    break;
                case "hour_minute":
                    hour_minute = reader.nextInt();
                    break;
                case "command":
                    command = reader.nextInt();
                    break;
                case "weekdays":
                    reader.beginArray();
                    int i = 0;
                    while (reader.hasNext()) {
                        weekdays[i++] = reader.nextBoolean();
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (uuid == null)
            throw new ClassNotFoundException("Old format!");

        if (executable_uid == null)
            throw new ClassNotFoundException();
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
        if (alarmOnDevice != null)
            alarmOnDevice.fromCache = true;
    }

    public void computeNextAlarmUnixTime(long currentTime) {
        if (type == TYPE_ONCE) {
            next_execution_unix_time = absolute_date.getTime();
            return;
        }
        Calendar calendar_current = Calendar.getInstance();
        Calendar calendar_start = Calendar.getInstance();
        calendar_current.setTimeInMillis(currentTime);
        calendar_start.setTimeInMillis(currentTime);
        final int day = calendar_start.get(Calendar.DAY_OF_WEEK) - 1; // start with 1: Sunday
        if (hour_minute != -1) {
            calendar_start.set(Calendar.HOUR_OF_DAY, getHour(hour_minute));
            calendar_start.set(Calendar.MINUTE, getMinute(hour_minute));
            for (int additionalDays = 0; additionalDays < 8; ++additionalDays) {
                if (weekdays[(additionalDays + day) % 7]) {
                    if (calendar_start.before(calendar_current)) {
                        calendar_start.add(Calendar.HOUR_OF_DAY, 24);
                        continue;
                    }
                    next_execution_unix_time = calendar_start.getTimeInMillis();
                    return;
                } else
                    calendar_start.add(Calendar.HOUR_OF_DAY, 24);
            }
        }
    }
}
