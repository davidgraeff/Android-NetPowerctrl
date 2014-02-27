package oly.netpowerctrl.network;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Executor;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all scenes to query.
 */
public class DeviceQuery {
    private List<DeviceInfo> devices_to_observe;
    private DeviceUpdateStateOrTimeout target;
    private Handler timeoutHandler = new Handler();

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceQuery.this);
            if (target == null)
                return;

            for (DeviceInfo di : devices_to_observe) {
                di.reachable = false;
                target.onDeviceTimeout(di);
            }
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceQuery.this);
            target.onDeviceQueryFinished(devices_to_observe);
        }
    };

    private Runnable requeryRunnable = new Runnable() {
        @Override
        public void run() {
            Log.w("Query", "requery");
            for (DeviceInfo di : devices_to_observe) {
                Executor.sendQuery(di);
            }
        }
    };

    public DeviceQuery(DeviceUpdateStateOrTimeout target, DeviceInfo device_to_observe) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>();
        devices_to_observe.add(device_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        Executor.sendQuery(device_to_observe);
        timeoutHandler.postDelayed(requeryRunnable, 300);
        timeoutHandler.postDelayed(requeryRunnable, 600);
        timeoutHandler.postDelayed(requeryRunnable, 1200);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);
    }

    public DeviceQuery(DeviceUpdateStateOrTimeout target, Collection<DeviceInfo> devices_to_observe) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>(devices_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        timeoutHandler.postDelayed(requeryRunnable, 300);
        timeoutHandler.postDelayed(requeryRunnable, 600);
        timeoutHandler.postDelayed(requeryRunnable, 1200);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);

        // Send out broadcast
        for (DeviceInfo di : devices_to_observe)
            Executor.sendQuery(di);
    }

    /**
     * Issues a broadcast query. If there is no response to that for all
     * configured devices, we will also do a device specific query.
     *
     * @param target
     */
    public DeviceQuery(DeviceUpdateStateOrTimeout target) {
        this.target = target;
        NetpowerctrlApplication.getDataController().clearNewDevices();
        this.devices_to_observe = new ArrayList<DeviceInfo>(NetpowerctrlApplication.getDataController().configuredDevices);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        timeoutHandler.postDelayed(requeryRunnable, 600);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);
        Executor.sendBroadcastQuery();
    }

    /**
     * Return true if all devices responded and this DeviceQuery object
     * have to be removed.
     *
     * @param received_data The DeviceInfo object all observes should be notified of.
     */
    public boolean notifyObservers(DeviceInfo received_data) {
        Iterator<DeviceInfo> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            DeviceInfo device_to_observe = it.next();
            if (device_to_observe.equals(received_data)) {
                it.remove();
                if (target != null)
                    target.onDeviceUpdated(received_data);
            }
        }
        if (devices_to_observe.isEmpty()) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutHandler.removeCallbacks(requeryRunnable);
            if (target != null)
                target.onDeviceQueryFinished(devices_to_observe);
            return true;
        }
        return false;
    }

    /**
     * Called right before this object is removed from the Application list
     * of DeviceQueries because the listener service has been shutdown. All
     * remaining device queries of this object have to timeout now.
     */
    public void finishWithTimeouts() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        for (DeviceInfo di : devices_to_observe) {
            di.reachable = false;
            target.onDeviceTimeout(di);
        }
        target.onDeviceQueryFinished(devices_to_observe);
    }
}
