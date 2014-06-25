package oly.netpowerctrl.alarms;

import android.os.Handler;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Control all configured alarms
 */
public class TimerController {
    private List<Alarm> alarms = new ArrayList<>();
    private List<Alarm> available_alarms = new ArrayList<>();
    private IAlarmsSave storage;
    private boolean requestActive = false;
    private final WeakHashMap<IAlarmsUpdated, Boolean> observers = new WeakHashMap<>();
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
                if (!replaced(alarms, new_alarm)) {
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
        PluginInterface p = alarm.port.device.getPluginInterface(NetpowerctrlApplication.getService());
        p.removeAlarm(alarm, callback);
    }

    public void setStorage(IAlarmsSave storage) {
        this.storage = storage;
    }

    public interface IAlarmsUpdated {
        // Return false to get removed from the observer list
        boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress);
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

    public void requestData() {

        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null) {
            NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
                @Override
                public boolean onServiceReady() {
                    requestData(NetpowerctrlApplication.getService());
                    return false;
                }

                @Override
                public void onServiceFinished() {

                }
            });
        } else {
            requestData(service);
        }
    }

    /**
     * Call this method to update the list of alarms
     *
     * @param service The main service
     */
    public void requestData(NetpowerctrlService service) {
        if (requestActive)
            return;

        requestActive = true;
        available_alarms.clear();

        // Flag all alarms as from-cache
        for (Alarm alarm : alarms)
            alarm.fromCache = true;
        notifyObservers(false, true);

        service.requestAllAlarms();
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

    public interface IAlarmsSave {
        void alarmsSave(TimerController alarms);
    }
}
