package oly.netpowerctrl.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.pluginservice.PluginInterface;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.WakeLocker;

/**
 * Control all configured alarms
 */
public class TimerCollection extends CollectionWithStorableItems<TimerCollection, Timer> {
    private static final String TAG = "TimerCollection";
    public onCollectionUpdated<DeviceCollection, Device> deviceObserver = new onCollectionUpdated<DeviceCollection, Device>() {
        @Override
        public boolean updated(@NonNull DeviceCollection deviceCollection, Device device, @NonNull ObserverUpdateActions action, int position) {
            if (action != ObserverUpdateActions.RemoveAllAction && action != ObserverUpdateActions.RemoveAction)
                return true;

            if (device == null) {
                removeAll();
                return true;
            }

            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                for (int index = items.size() - 1; index >= 0; --index) {
                    Timer timer = items.get(index);
                    if (timer.executable_uid.equals(devicePort.getUid())) {
                        removeFromCache(index);
                    }
                }
            }
            device.releaseDevicePorts();
            return true;
        }
    };
    private boolean requestActive = false;
    private final Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            for (int index = items.size() - 1; index >= 0; --index) {
                Timer timer = items.get(index);
                if (timer.isFromCache()) {
                    requestActive = false;
                    removeFromCache(index);
                }
            }

            if (requestActive) {
                requestActive = false;
                notifyObservers(null, ObserverUpdateActions.ConnectionUpdateAction, -1);
            }
            saveAll();
        }
    };

    public static void setupAndroidAlarm(Context context) {
        // We use either the current time or if it the same as the already saved next alarm time, current time + 1
        long current = System.currentTimeMillis();
        long nextAlarmTimestamp = SharedPrefs.getNextAlarmCheckTimestamp(context);
        if (current == nextAlarmTimestamp)
            ++current;

        Timer nextTimer = null;

        for (Timer timer : AppData.getInstance().timerCollection.getItems()) {
            if (timer.alarmOnDevice != null)
                continue;

            timer.computeNextAlarmUnixTime(current);
            if (timer.next_execution_unix_time > current &&
                    (nextTimer == null || timer.next_execution_unix_time < nextTimer.next_execution_unix_time)) {
                nextTimer = timer;
            }
        }

        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PluginService.class);
        PendingIntent pi = PendingIntent.getService(context, 1191, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

        mgr.cancel(pi);

        if (nextTimer == null) {
            if (nextAlarmTimestamp > 0) {
                // Disable the manifest entry for the BootCompletedReceiver. This way the app won't unnecessarly wake up after boot.
                ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
                PackageManager pm = context.getPackageManager();
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                // Reset next alarm time cache
                SharedPrefs.setNextAlarmCheckTimestamp(context, 0);
            }
            return;
        }

        SharedPrefs.setNextAlarmCheckTimestamp(context, nextTimer.next_execution_unix_time);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextTimer.next_execution_unix_time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Logging.getInstance().logAlarm("Next alarm: " + nextTimer.getTargetName() + "\nOn: " + sdf.format(calendar.getTime()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mgr.setExact(AlarmManager.RTC_WAKEUP, nextTimer.next_execution_unix_time, pi);
        else
            mgr.set(AlarmManager.RTC_WAKEUP, nextTimer.next_execution_unix_time, pi);

        // Enable the manifest entry for the BootCompletedReceiver. This way alarms are setup again after a reboot.
        ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void checkAndExecuteAlarm() {
        long nextAlarmTimestamp = SharedPrefs.getNextAlarmCheckTimestamp(App.instance);

        { // Check if current time matches somehow the setup next alarm time
            long maxDiffms = 50000;
            long current = System.currentTimeMillis();

            if (nextAlarmTimestamp > current || nextAlarmTimestamp + maxDiffms < current) {
                TimerCollection.setupAndroidAlarm(App.instance);
                return;
            }
        } // From here on, we use nextAlarmTimestamp as current

        onExecutionFinished executionFinished = new onExecutionFinished() {
            @Override
            public void onExecutionProgress(int success, int errors, int all) {
                if (success + errors >= all) {
                    Logging.getInstance().logAlarm("Alarm items (OK, FAIL):\n" + String.valueOf(success) + ", " + String.valueOf(errors));
                    WakeLocker.release();
                }
            }
        };

        boolean alarmExecuted = false;

        for (Timer timer : AppData.getInstance().timerCollection.getItems()) {
            if (timer.alarmOnDevice != null)
                continue;

            timer.computeNextAlarmUnixTime(nextAlarmTimestamp);

            if (timer.next_execution_unix_time == nextAlarmTimestamp) {
                Executable executable = AppData.getInstance().findExecutable(timer.executable_uid);

                if (executable != null)
                    Logging.getInstance().logAlarm("Alarm: " + executable.getTitle());
                else {
                    Logging.getInstance().logAlarm("Alarm: Executable does not exist!");
                    continue;
                }

                alarmExecuted = true;

                if (executable instanceof DevicePort) {
                    WakeLocker.acquire(App.instance);
                    AppData.getInstance().execute((DevicePort) executable, timer.command, executionFinished);
                } else if (executable instanceof Scene) {
                    WakeLocker.acquire(App.instance);
                    AppData.getInstance().execute((Scene) executable, executionFinished);
                }
            }
        }

        if (!alarmExecuted)
            Logging.getInstance().logAlarm("Armed alarm not found!");

        // Setup new alarm if one got executed
        TimerCollection.setupAndroidAlarm(App.instance);
    }

    public boolean isRequestActive() {
        return requestActive;
    }

    public int countAllDeviceAlarms() {
        return items.size();
    }

    private int replaced_at(Timer timer) {
        for (int i = 0; i < items.size(); ++i) {
            Timer a = items.get(i);
            if (a.equals(timer)) {
                items.set(i, timer);
                notifyObservers(timer, ObserverUpdateActions.UpdateAction, i);
                return i;
            }
        }
        return -1;
    }

    /**
     * Called by plugins to propagate alarms.
     *
     * @param new_timers All alarms of the plugin.
     */
    public void alarmsFromPluginOtherThread(final List<Timer> new_timers) {
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1500);
        h.post(new Runnable() {
            @Override
            public void run() {
                for (Timer new_timer : new_timers) {
                    int i = replaced_at(new_timer);
                    if (i == -1 && new_timer.executable_uid != null) {
                        items.add(new_timer);
                        notifyObservers(null, ObserverUpdateActions.AddAction, items.size() - 1);
                    }
                }
            }
        });
    }

    public void addAlarm(Timer alarm) {
        int i = replaced_at(alarm);
        if (i == -1 && alarm.executable_uid != null) {
            items.add(alarm);
            i = items.size() - 1;
            notifyObservers(alarm, ObserverUpdateActions.AddAction, i);
        } else {
            notifyObservers(alarm, ObserverUpdateActions.UpdateAction, i);
        }
        save(alarm);

        if (alarm.alarmOnDevice == null)
            setupAndroidAlarm(App.instance);
    }

    void removeAlarm(final Timer timer, final onHttpRequestResult callback) {
        if (timer.executable instanceof DevicePort && timer.alarmOnDevice != null) {
            Device device = ((DevicePort) timer.executable).device;
            PluginService.getService().wakeupPlugin(device);
            PluginInterface p = (PluginInterface) device.getPluginInterface();
            p.removeAlarm(timer, new onHttpRequestResult() {
                @Override
                public void httpRequestResult(DevicePort oi, boolean success, String error_message) {
                    if (success)
                        removeAlarmFromDisk(timer);
                    if (callback != null)
                        callback.httpRequestResult(oi, success, error_message);
                }

                @Override
                public void httpRequestStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {
                    if (callback != null)
                        callback.httpRequestStart(oi);
                }
            });
        } else {
            removeAlarmFromDisk(timer);
        }
    }

    void removeAlarmFromDisk(Timer timer) {
        remove(timer);
        for (int i = 0; i < items.size(); ++i) {
            if (items.get(i) == timer) {
                items.remove(i);
                notifyObservers(timer, ObserverUpdateActions.RemoveAction, i);
                break;
            }
        }
    }

    public void removeFromCache(int position) {
        if (position != -1) {
            remove(items.get(position));
            items.remove(position);
            notifyObservers(null, ObserverUpdateActions.RemoveAction, position);
        }
    }

    public boolean refresh(PluginService service) {
        if (requestActive)
            return true;

        App.getMainThreadHandler().postDelayed(notifyRunnable, 1500);

        // Flag all alarms as from-cache
        HashSet<String> alarm_uuids = new HashSet<>();
        for (Timer timer : items) {
            timer.markFromCache();
            if (timer.alarmOnDevice != null)
                alarm_uuids.add(timer.executable_uid);
        }

        requestActive = true;
        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);

        List<DevicePort> alarm_ports = new ArrayList<>();

        service.wakeupAllDevices();

        DeviceCollection c = AppData.getInstance().deviceCollection;
        // Put all ports of all devices into the list alarm_ports.
        // If a port is referenced by the alarm_uuids hashSet, it will be put in front of the list
        // to refresh that port first.
        for (Device device : c.getItems()) {
            // Request all alarm_uuids may be called before all plugins responded
            if (!device.isEnabled())
                continue;

            // Request alarm_uuids for every port
            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                final DevicePort port = it.next();
                if (port.isHidden())
                    continue;

                if (alarm_uuids.contains(port.getUid()))
                    alarm_ports.add(0, port); // add in front of all alarm_uuids
                else
                    alarm_ports.add(port);
            }
            device.releaseDevicePorts();
        }

        for (DevicePort port : alarm_ports) {
            PluginInterface i = (PluginInterface) port.device.getPluginInterface();
            //TODO eigentlich sollen alle getPluginInterface() etwas zurückgeben. Jedoch laden Erweiterungen nicht schnell genug für diesen Aufruf hier
            if (i != null)
                i.requestAlarms(port, this);
        }

        return requestActive;
    }

    public void abortRequest() {
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        requestActive = false;
        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);
    }

    public void removeAll() {
        int b = items.size();
        // Delete all alarms
        for (Timer timer : getItems())
            removeAlarm(timer, null);
        items.clear();
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, b);
    }

    @Override
    public String type() {
        return "alarms";
    }

}
