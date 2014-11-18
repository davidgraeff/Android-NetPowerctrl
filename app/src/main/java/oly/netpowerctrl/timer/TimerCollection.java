package oly.netpowerctrl.timer;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onHttpRequestResult;

/**
 * Control all configured alarms
 */
public class TimerCollection extends CollectionWithStorableItems<TimerCollection, Timer> {
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
            requestActive = false;
            saveAll();
        }
    };


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
        h.postDelayed(notifyRunnable, 1200);
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
            ListenService.getService().setupAndroidAlarm();
    }

    public List<Timer> getAvailableDeviceAlarms() {
        return available_timers;
    }

    void removeAlarm(Timer timer, onHttpRequestResult callback) {
        if (timer.executable instanceof DevicePort && timer.deviceAlarm) {
            Device device = ((DevicePort) timer.executable).device;
            ListenService.getService().wakeupPlugin(device);
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


    public boolean refresh(ListenService service) {
        if (requestActive)
            return true;

        App.getMainThreadHandler().postDelayed(notifyRunnable, 1200);

        available_timers.clear();

        // Flag all alarms as from-cache
        HashSet<String> alarm_uuids = new HashSet<>();
        for (Timer timer : items) {
            timer.markFromCache();
            if (timer.deviceAlarm)
                alarm_uuids.add(timer.executable_uid);
        }

        notifyObservers(null, ObserverUpdateActions.UpdateAction, -1);

        requestActive = true;

        List<DevicePort> alarm_ports = new ArrayList<>();

        service.wakeupAllDevices(false);

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
