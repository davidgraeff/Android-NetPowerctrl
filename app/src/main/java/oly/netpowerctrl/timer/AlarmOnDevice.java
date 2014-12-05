package oly.netpowerctrl.timer;

/**
 * Used in timer. If !=null the alarm is stored on a device and will not be executed by android.
 */
public class AlarmOnDevice {
    /**
     * Not armed device alarm that is available to be programmed. It will not be shown on the
     * alarm list. To distinguish between a free alarm slot and a programmed but "disabled" alarm,
     * "freeDeviceAlarm" is only set, if the alarm values are the default values. For anel devices
     * this is: All weekdays, on-time from 00:00 to 23:59.
     */
    public boolean freeDeviceAlarm;
    // If this alarm is not from device (yet) but only from the cache, this flag is set.
    public boolean fromCache = false;
    public byte portId;
    public byte timerId;

    public AlarmOnDevice(boolean fromCache) {
        this.fromCache = fromCache;
    }
}
