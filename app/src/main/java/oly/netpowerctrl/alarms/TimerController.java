package oly.netpowerctrl.alarms;

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
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Control all configured alarms
 */
public class TimerController {
    private final WeakHashMap<IAlarmsUpdated, Boolean> observers = new WeakHashMap<>();
    private List<Alarm> alarms = new ArrayList<>();
    private List<Alarm> available_alarms = new ArrayList<>();
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
        return alarms.size() + available_alarms.size();
    }

    private boolean replaced(List<Alarm> list, Alarm alarm) {
        for (int i = 0; i < list.size(); ++i) {
            Alarm a = list.get(i);
            if (a.id == alarm.id && a.port_id.equals(alarm.port_id)) {
                list.set(i, alarm);
                return true;
            }
        }
        return false;
    }

    public void save() {
        // Remove cache-only entries before saving
        Iterator<Alarm> it = alarms.iterator();
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
     * @param new_alarms All alarms of the plugin.
     */
    synchronized public void alarmsFromPlugin(List<Alarm> new_alarms) {
        for (Alarm new_alarm : new_alarms) {
            if (new_alarm.freeDeviceAlarm) {
                if (!replaced(available_alarms, new_alarm)) {
                    available_alarms.add(new_alarm);
                }
            } else {
                if (!replaced(alarms, new_alarm) && new_alarm.port_id != null) {
                    alarms.add(new_alarm);
                }
            }
        }
        Handler h = NetpowerctrlApplication.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1200);
        h.postDelayed(notifyRunnableNow, 100);
    }

    public List<Alarm> getAvailableDeviceAlarms() {
        return available_alarms;
    }

    public void removeAlarm(Alarm alarm, AsyncRunnerResult callback) {
        PluginInterface p = alarm.port.device.getPluginInterface(NetpowerctrlService.getService());
        p.removeAlarm(alarm, callback);
    }

    public void setStorage(IAlarmsSave storage) {
        this.storage = storage;
    }

    public void removeFromCache(int position) {
        if (position != -1)
            alarms.remove(position);
    }

    public int getCount() {
        return alarms.size();
    }

    public Alarm getItem(int i) {
        return alarms.get(i);
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

        available_alarms.clear();

        // Flag all alarms as from-cache
        HashSet<UUID> alarm_uuids = new HashSet<>();
        for (Alarm alarm : alarms) {
            alarm.fromCache = true;
            alarm_uuids.add(alarm.port_id);
        }

        notifyObservers(false, true);

        requestActive = service.requestAllAlarms(alarm_uuids, this);
        if (!requestActive)
            notifyObservers(false, false);

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
        alarms.clear();

        if (reader == null)
            return;

        reader.beginArray();
        while (reader.hasNext()) {
            try {
                alarms.add(Alarm.fromJSON(reader));
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
        for (Alarm alarm : alarms) {
            alarm.toJSON(writer);
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
