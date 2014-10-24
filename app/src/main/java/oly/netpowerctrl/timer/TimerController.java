package oly.netpowerctrl.timer;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onHttpRequestResult;

/**
 * Control all configured alarms
 */
public class TimerController extends CollectionWithStorableItems<TimerController, Timer> {
    private final List<Timer> available_timers = new ArrayList<>();
    private boolean requestActive = false;
    private final Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            requestActive = false;
            saveAll();
        }
    };
    private long lastExecuted;
    private final Runnable notifyRunnableNow = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - lastExecuted < 200)
                return;
            lastExecuted = System.currentTimeMillis();
            notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);
        }
    };

    public boolean isRequestActive() {
        return requestActive;
    }

    public int countAllDeviceAlarms() {
        return items.size() + available_timers.size();
    }

    private boolean not_replaced(List<Timer> list, Timer timer) {
        for (int i = 0; i < list.size(); ++i) {
            Timer a = list.get(i);
            if (a.id == timer.id && a.port_id.equals(timer.port_id)) {
                list.set(i, timer);
                return false;
            }
        }
        return true;
    }

    @Override
    public void saveAll() {
        if (storage == null)
            return;

        Iterator<Timer> it = items.iterator();

        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);

        it = items.iterator();
        while (it.hasNext()) {
            storage.save(this, it.next());
        }
    }

    /**
     * Called by plugins to propagate alarms.
     *
     * @param new_timers All alarms of the plugin.
     */
    synchronized public void alarmsFromPlugin(List<Timer> new_timers) {
        for (Timer new_timer : new_timers) {
            if (new_timer.freeDeviceAlarm) {
                if (not_replaced(available_timers, new_timer)) {
                    available_timers.add(new_timer);
                }
            } else {
                if (not_replaced(items, new_timer) && new_timer.port_id != null) {
                    items.add(new_timer);
                }
            }
        }
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1200);
        h.postDelayed(notifyRunnableNow, 100);
    }

    public List<Timer> getAvailableDeviceAlarms() {
        return available_timers;
    }

    void removeAlarm(Timer timer, onHttpRequestResult callback) {
        PluginInterface p = timer.port.device.getPluginInterface();
        p.removeAlarm(timer, callback);
    }

    public void removeFromCache(int position) {
        if (position != -1)
            items.remove(position);
    }


    public boolean refresh(ListenService service) {
        if (requestActive)
            return true;

        App.getMainThreadHandler().postDelayed(notifyRunnable, 1200);

        available_timers.clear();

        // Flag all alarms as from-cache
        HashSet<String> alarm_uuids = new HashSet<>();
        for (Timer timer : items) {
            timer.fromCache = true;
            alarm_uuids.add(timer.port_id);
        }

        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);

        if (service.isNetworkReducedMode() || requestActive) {
            return requestActive;
        }

        requestActive = true;

        List<DevicePort> alarm_ports = new ArrayList<>();

        DeviceCollection c = AppData.getInstance().deviceCollection;
        // Put all ports of all devices into the list alarm_ports.
        // If a port is referenced by the alarm_uuids hashSet, it will be put in front of the list
        // to refresh that port first.
        for (Device di : c.getItems()) {
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

                if (alarm_uuids.contains(port.getUid()))
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
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        requestActive = false;
        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);
    }

    public void clear() {
        // Delete all alarms
        for (Timer timer : getItems())
            removeAlarm(timer, null);
        items.clear();
        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);
    }

    @Override
    public String type() {
        return "alarms";
    }
}
