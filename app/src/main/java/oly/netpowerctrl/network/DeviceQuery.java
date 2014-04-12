package oly.netpowerctrl.network;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all scenes to query.
 */
public class DeviceQuery {
    private List<DeviceInfo> devices_to_observe;
    private DeviceQueryResult target;
    private Handler timeoutHandler = new Handler(NetpowerctrlApplication.instance.getMainLooper());

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceQuery.this);
            if (target == null)
                return;

            for (DeviceInfo di : devices_to_observe) {
                di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
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
                sendQuery(di);
            }
        }
    };

    public DeviceQuery(DeviceQueryResult target, DeviceInfo device_to_observe) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>();
        devices_to_observe.add(device_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        sendQuery(device_to_observe);
        timeoutHandler.postDelayed(requeryRunnable, 300);
        timeoutHandler.postDelayed(requeryRunnable, 600);
        timeoutHandler.postDelayed(requeryRunnable, 1200);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);
    }

    public DeviceQuery(DeviceQueryResult target, Collection<DeviceInfo> devices_to_observe) {
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
            sendQuery(di);
    }

    /**
     * Issues a broadcast query. If there is no response to that for all
     * configured devices, we will also do a device specific query.
     *
     * @param target
     */
    public DeviceQuery(DeviceQueryResult target) {
        this.target = target;
        NetpowerctrlApplication.getDataController().clearNewDevices();
        this.devices_to_observe = new ArrayList<DeviceInfo>(NetpowerctrlApplication.getDataController().configuredDevices);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        timeoutHandler.postDelayed(requeryRunnable, 600);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);
        NetpowerctrlApplication.getService().sendBroadcastQuery();
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
            boolean eq = received_data.configured ? device_to_observe.equals(received_data) :
                    device_to_observe.equalsFunctional(received_data);
            if (eq) {
                it.remove();
                if (target != null)
                    target.onDeviceUpdated(received_data);
                break;
            }
        }
        return checkIfDone();
    }

    public boolean notifyObservers(String device_name) {
        Iterator<DeviceInfo> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            DeviceInfo device_to_observe = it.next();
            boolean eq = device_name.equals(device_to_observe.DeviceName);
            if (eq) {
                it.remove();
                if (target != null)
                    target.onDeviceUpdated(device_to_observe);
                break;
            }
        }
        return checkIfDone();
    }

    public void addDevice(DeviceInfo device, boolean resetTimeout) {
        devices_to_observe.add(device);

        if (!resetTimeout)
            return;
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, 1500);
    }

    private boolean checkIfDone() {
        if (devices_to_observe.isEmpty()) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutHandler.removeCallbacks(requeryRunnable);
            if (target != null)
                target.onDeviceQueryFinished(devices_to_observe);
            return true;
        }
        return false;
    }

//    if (deviceName.equals(device.DeviceName)) {
//        test_state = TestStates.TEST_INIT;
//    }

    /**
     * Called right before this object is removed from the Application list
     * of DeviceQueries because the listener service has been shutdown. All
     * remaining device queries of this object have to timeout now.
     */
    public void finishWithTimeouts() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        for (DeviceInfo di : devices_to_observe) {
            di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
            target.onDeviceTimeout(di);
        }
        target.onDeviceQueryFinished(devices_to_observe);
    }

    private void sendQuery(DeviceInfo di) {
        PluginInterface remote = di.getPluginInterface();
        boolean reachable = remote != null;

        if (reachable) {
            remote.requestData(di);
        } else {
            di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_plugin_not_installed));
        }
    }
}
