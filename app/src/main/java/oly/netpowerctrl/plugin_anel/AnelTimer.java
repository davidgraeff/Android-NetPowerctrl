package oly.netpowerctrl.plugin_anel;

import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.timer.AlarmOnDevice;
import oly.netpowerctrl.timer.Timer;

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

    public AlarmOnDevice alarmOnDevice = new AlarmOnDevice(false);
    // Temporary
    public Executable Executable;
    public int type = Timer.TYPE_RANGE_ON_WEEKDAYS;
    boolean enabled;
    private String uniqueTimerID;

    public void computeUniqueTimerID() {
        uniqueTimerID = Executable.getUid() + "-" + String.valueOf(alarmOnDevice.timerId);
    }

    public String getUniqueTimerID(int command) {
        return uniqueTimerID + "-" + String.valueOf(command);
    }

    public String getUniqueTimerID() {
        return uniqueTimerID;
    }

    public boolean isFree() {
        return (!enabled && hour_minute_start == 0 && hour_minute_stop == 23 * 60 + 59);
    }

    public boolean isUnused(int command) {
        if (command == Executable.ON)
            return hour_minute_start <= 0;
        else if (command == Executable.OFF)
            return hour_minute_stop == 23 * 60 + 59 || hour_minute_stop == -1;
        return false;
    }

    public void updateBy(Timer timer) {
        System.arraycopy(timer.weekdays, 0, weekdays, 0, weekdays.length);

        if (timer.command == Executable.ON) {
            hour_minute_start = timer.hour_minute;
        } else if (timer.command == Executable.OFF) {
            hour_minute_stop = timer.hour_minute;
        }

        alarmOnDevice.freeDeviceAlarm = isFree();
        // This may disable two regular alarms (one on / one off)
        // Unfortunately anel devices only support on/off combinations
        enabled = !alarmOnDevice.freeDeviceAlarm && timer.enabled;

    }

}
