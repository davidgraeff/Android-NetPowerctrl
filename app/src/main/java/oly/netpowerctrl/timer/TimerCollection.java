package oly.netpowerctrl.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
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
import oly.netpowerctrl.utils.WakeLocker;

/**
 * Control all configured alarms
 */
public class TimerCollection extends CollectionWithStorableItems<TimerCollection, Timer> {
    private static final String TAG = "TimerCollection";
    private final List<Timer> available_timers = new ArrayList<>();
    public onCollectionUpdated<DeviceCollection, Device> deviceObserver = new onCollectionUpdated<DeviceCollection, Device>() {
        @Override
        public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action, int position) {
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
                if (timer.fromCache) {
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
        long current = System.currentTimeMillis();
        Timer.NextAlarm nextTimerTime = null;
        Timer nextTimer = null;

        for (Timer timer : AppData.getInstance().timerCollection.getItems()) {
            if (timer.deviceAlarm)
                continue;

            Timer.NextAlarm alarm = timer.getNextAlarmUnixTime(current);
            if (alarm.unix_time > current && (nextTimerTime == null || alarm.unix_time < nextTimerTime.unix_time)) {
                nextTimerTime = alarm;
                nextTimer = timer;
            }
        }

        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PluginService.class);
        PendingIntent pi = PendingIntent.getService(context, 1191, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mgr.cancel(pi);

        if (nextTimerTime == null)
            return;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextTimerTime.unix_time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Log.w(TAG, "Next alarm " + sdf.format(calendar.getTime()) + " " + nextTimer.getTargetName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mgr.setExact(AlarmManager.RTC_WAKEUP, nextTimerTime.unix_time, pi);
        else
            mgr.set(AlarmManager.RTC_WAKEUP, nextTimerTime.unix_time, pi);
    }

    public static void checkAndExecuteAlarm() {
        Toast.makeText(App.instance, "checkAndExecuteAlarm", Toast.LENGTH_LONG).show();

        onExecutionFinished executionFinished = new onExecutionFinished() {
            @Override
            public void onExecutionProgress(int current, int all) {
                Toast.makeText(App.instance, "checkAndExecuteAlarm:exec:finished", Toast.LENGTH_LONG).show();
                if (current >= all)
                    WakeLocker.release();
            }
        };
        long current = System.currentTimeMillis();
        for (Timer timer : AppData.getInstance().timerCollection.getItems()) {
            if (timer.deviceAlarm)
                continue;

            Timer.NextAlarm nextAlarm = timer.getNextAlarmUnixTime(current);
            if (current - 50 < nextAlarm.unix_time && current + 50 > nextAlarm.unix_time) {
                WakeLocker.acquire(App.instance);
                Executable executable = AppData.getInstance().findExecutable(timer.executable_uid);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(nextAlarm.unix_time);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Log.w(TAG, "Now alarm " + sdf.format(calendar.getTime()) + " " + executable.getTitle());
                Toast.makeText(App.instance, "checkAndExecuteAlarm:exec " + sdf.format(calendar.getTime()) + " " + executable.getTitle(), Toast.LENGTH_LONG).show();

                if (executable instanceof DevicePort)
                    AppData.getInstance().execute((DevicePort) executable, nextAlarm.command, executionFinished);
                else if (executable instanceof Scene) {
                    AppData.getInstance().execute((Scene) executable, executionFinished);
                } else
                    WakeLocker.release();
            }
        }
        TimerCollection.setupAndroidAlarm(App.instance);
    }

    public boolean isRequestActive() {
        return requestActive;
    }

    public int countAllDeviceAlarms() {
        return items.size() + available_timers.size();
    }

    private int replaced_at(List<Timer> list, Timer timer) {
        for (int i = 0; i < list.size(); ++i) {
            Timer a = list.get(i);
            if (a.id == timer.id && a.executable_uid.equals(timer.executable_uid)) {
                list.set(i, timer);
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
                updateTimers(new_timers);
            }
        });
    }

    public void updateTimers(List<Timer> new_timers) {
        for (Timer new_timer : new_timers) {
            int i;
            if (new_timer.freeDeviceAlarm) {
                i = replaced_at(available_timers, new_timer);
                if (i == -1) {
                    available_timers.add(new_timer);
                    notifyObservers(null, ObserverUpdateActions.ConnectionUpdateAction, -1);
                }
            } else {
                i = replaced_at(items, new_timer);
                if (i == -1 && new_timer.executable_uid != null) {
                    items.add(new_timer);
                    notifyObservers(null, ObserverUpdateActions.AddAction, items.size() - 1);
                }
            }
        }
    }

    public void addAlarm(Timer alarm) {
        int i = replaced_at(items, alarm);
        if (i == -1 && alarm.executable_uid != null) {
            items.add(alarm);
            i = items.size() - 1;
            notifyObservers(alarm, ObserverUpdateActions.AddAction, i);
        } else {
            notifyObservers(alarm, ObserverUpdateActions.UpdateAction, i);
        }
        save(alarm);

        if (!alarm.deviceAlarm)
            setupAndroidAlarm(App.instance);
    }

    public List<Timer> getAvailableDeviceAlarms() {
        return available_timers;
    }

    void removeAlarm(Timer timer, onHttpRequestResult callback) {
        if (timer.executable instanceof DevicePort && timer.deviceAlarm) {
            Device device = ((DevicePort) timer.executable).device;
            PluginService.getService().wakeupPlugin(device);
            PluginInterface p = (PluginInterface) device.getPluginInterface();
            p.removeAlarm(timer, callback);
        } else {
            remove(timer);
            for (int i = 0; i < items.size(); ++i) {
                if (items.get(i).id == timer.id) {
                    items.remove(i);
                    notifyObservers(timer, ObserverUpdateActions.RemoveAction, i);
                    break;
                }
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

        available_timers.clear();

        // Flag all alarms as from-cache
        HashSet<String> alarm_uuids = new HashSet<>();
        for (Timer timer : items) {
            timer.markFromCache();
            if (timer.deviceAlarm)
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
                if (port.Disabled)
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
