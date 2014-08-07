package oly.netpowerctrl.timer;

import android.os.Handler;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Control all configured alarms
 */
public class TimerController {
    private final WeakHashMap<IAlarmsUpdated, Boolean> observers = new WeakHashMap<>();
    private List<Timer> timers = new ArrayList<>();
    private List<Timer> available_timers = new ArrayList<>();
    private IAlarmsSave storage;
    private boolean requestActive = false;
    private Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            requestActive = false;
            save();
        }
    };
    private long lastExecuted;
    private Runnable notifyRunnableNow = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - lastExecuted < 200)
                return;
            lastExecuted = System.currentTimeMillis();
            notifyObservers(true, true);
        }
    };

    public int countAllDeviceAlarms() {
        return timers.size() + available_timers.size();
    }

    private boolean replaced(List<Timer> list, Timer timer) {
        for (int i = 0; i < list.size(); ++i) {
            Timer a = list.get(i);
            if (a.id == timer.id && a.port_id.equals(timer.port_id)) {
                list.set(i, timer);
                return true;
            }
        }
        return false;
    }

    public void save() {
        // Remove cache-only entries before saving
        Iterator<Timer> it = timers.iterator();
        while (it.hasNext()) {
            if (it.next().fromCache) {
                it.remove();
            }
        }

        notifyObservers(true, false);

        if (storage != null)
            storage.alarmsSave(this);
    }

    /**
     * Called by plugins to propagate alarms.
     *
     * @param new_timers All alarms of the plugin.
     */
    synchronized public void alarmsFromPlugin(List<Timer> new_timers) {
        for (Timer new_timer : new_timers) {
            if (new_timer.freeDeviceAlarm) {
                if (!replaced(available_timers, new_timer)) {
                    available_timers.add(new_timer);
                }
            } else {
                if (!replaced(timers, new_timer) && new_timer.port_id != null) {
                    timers.add(new_timer);
                }
            }
        }
        Handler h = NetpowerctrlApplication.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1200);
        h.postDelayed(notifyRunnableNow, 100);
    }

    public List<Timer> getAvailableDeviceAlarms() {
        return available_timers;
    }

    public void removeAlarm(Timer timer, AsyncRunnerResult callback) {
        PluginInterface p = timer.port.device.getPluginInterface();
        p.removeAlarm(timer, callback);
    }

    public void setStorage(IAlarmsSave storage) {
        this.storage = storage;
    }

    public void removeFromCache(int position) {
        if (position != -1)
            timers.remove(position);
    }

    public int getCount() {
        return timers.size();
    }

    public Timer getItem(int i) {
        return timers.get(i);
    }

    @SuppressWarnings("unused")
    public void registerObserver(IAlarmsUpdated o) {
        if (!observers.containsKey(o)) {
            observers.put(o, true);
        }
    }

    public boolean refresh(NetpowerctrlService service) {
        if (requestActive)
            return true;

        NetpowerctrlApplication.getMainThreadHandler().postDelayed(notifyRunnable, 1200);

        available_timers.clear();

        // Flag all alarms as from-cache
        HashSet<UUID> alarm_uuids = new HashSet<>();
        for (Timer timer : timers) {
            timer.fromCache = true;
            alarm_uuids.add(timer.port_id);
        }

        notifyObservers(false, true);

        if (service.isNetworkReducedMode() || requestActive) {
            notifyObservers(false, false);
            return requestActive;
        }

        requestActive = true;

        List<DevicePort> alarm_ports = new ArrayList<>();

        DeviceCollection c = NetpowerctrlApplication.getDataController().deviceCollection;
        // Put all ports of all devices into the list alarm_ports.
        // If a port is referenced by the alarm_uuids hashSet, it will be put in front of the list
        // to refresh that port first.
        for (Device di : c.devices) {
            // Request all alarm_uuids may be called before all plugins responded
            PluginInterface i = di.getPluginInterface();
            if (i == null || !di.isEnabled())
                continue;

            // Request alarm_uuids for every port
            di.lockDevicePorts();
            Iterator<DevicePort> it = di.getDevicePortIterator();
            while (it.hasNext()) {
                final DevicePort port = it.next();
                if (port.Disabled)
                    continue;

                if (alarm_uuids.contains(port.uuid))
                    alarm_ports.add(0, port); // add in front of all alarm_uuids
                else
                    alarm_ports.add(port);
            }
            di.releaseDevicePorts();
        }

        for (DevicePort port : alarm_ports) {
            PluginInterface i = port.device.getPluginInterface();
            i.requestAlarms(port, this);
        }

        return requestActive;
    }

    public void abortRequest() {
        Handler h = NetpowerctrlApplication.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        requestActive = false;
        notifyObservers(false, false);
    }

    public void unregisterObserver(IAlarmsUpdated o) {
        observers.remove(o);
    }

    private void notifyObservers(boolean addedOrRemoved, boolean inProgress) {
        Iterator<IAlarmsUpdated> it = observers.keySet().iterator();
        while (it.hasNext())
            if (!it.next().alarmsUpdated(addedOrRemoved, inProgress))
                it.remove();

    }

    public void fromJSON(JsonReader reader) throws IOException, IllegalStateException {
        timers.clear();

        if (reader == null)
            return;

        reader.beginArray();
        while (reader.hasNext()) {
            try {
                timers.add(Timer.fromJSON(reader));
            } catch (ClassNotFoundException | ParseException ignored) {
            }
        }
        reader.endArray();
    }

    /**
     * Return the json representation of all groups
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        return toJSON();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (Timer timer : timers) {
            timer.toJSON(writer);
        }
        writer.endArray();
    }

    public interface IAlarmsUpdated {
        // Return false to get removed from the observer list
        boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress);
    }

    public interface IAlarmsSave {
        void alarmsSave(TimerController alarms);
    }
}
