package oly.netpowerctrl.alarms;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.ServiceReady;

/**
 * Control all configured alarms
 */
public class TimerController {
    private List<Alarm> alarms = new ArrayList<>();
    private List<Alarm> available_alarms = new ArrayList<>();
    private final WeakHashMap<IAlarmsUpdated, Boolean> observers = new WeakHashMap<>();
    private Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            notifyObservers(true, false);
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

    /**
     * Called by plugins to propagate alarms.
     *
     * @param alarms All alarms of the plugin.
     */
    synchronized public void alarmsFromPlugin(List<Alarm> alarms) {
        for (Alarm alarm : alarms) {
            if (alarm.freeDeviceAlarm)
                available_alarms.add(alarm);
            else
                this.alarms.add(alarm);
        }
        Handler h = NetpowerctrlApplication.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1200);
        h.postDelayed(notifyRunnableNow, 100);
    }

    public List<Alarm> getAvailableDeviceAlarms() {
        return available_alarms;
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
        alarms.clear();
        available_alarms.clear();

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
}
