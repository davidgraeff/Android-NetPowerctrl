package oly.netpowerctrl.network;

import android.os.Handler;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Created by david on 16.04.14.
 */
public abstract class DeviceObserverBase {
    public void setDeviceQueryResult(DeviceQueryResult target) {
        this.target = target;
    }

    protected List<DeviceInfo> devices_to_observe;
    protected DeviceQueryResult target;
    protected Handler mainLoopHandler = new Handler(NetpowerctrlApplication.instance.getMainLooper());
    protected Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceObserverBase.this);
            if (target == null)
                return;

            for (DeviceInfo di : devices_to_observe) {
                di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
                target.onDeviceTimeout(di);
            }
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceObserverBase.this);
            target.onDeviceQueryFinished(devices_to_observe);
        }
    };

    protected abstract void doAction(DeviceInfo di);

    protected Runnable redoRunnable = new Runnable() {
        @Override
        public void run() {
            Log.w("DeviceObserverBase", "redo action");
            for (DeviceInfo di : devices_to_observe) {
                doAction(di);
            }
        }
    };

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
            if (device_to_observe.equalsByUniqueID(received_data)) {
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
        mainLoopHandler.removeCallbacks(timeoutRunnable);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);
    }

    private boolean checkIfDone() {
        if (devices_to_observe.isEmpty()) {
            mainLoopHandler.removeCallbacks(timeoutRunnable);
            mainLoopHandler.removeCallbacks(redoRunnable);
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
        mainLoopHandler.removeCallbacks(timeoutRunnable);
        for (DeviceInfo di : devices_to_observe) {
            di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
            //di.setUpdatedNow();
            if (target != null)
                target.onDeviceTimeout(di);
        }
        if (target != null)
            target.onDeviceQueryFinished(devices_to_observe);
    }
}
