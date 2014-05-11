package oly.netpowerctrl.network;

import android.os.Handler;

import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;

/**
 * This base class is used by the device query and device-resend-command class.
 * It will be registered on the main application to receive device updates and
 * provide the result of a query/a sending action to the DeviceQueryResult object.
 */
public abstract class DeviceObserverBase {
    public void setDeviceQueryResult(DeviceObserverResult target) {
        this.target = target;
    }

    List<DeviceInfo> devices_to_observe;
    private DeviceObserverResult target;
    final Handler mainLoopHandler = new Handler(NetpowerctrlApplication.instance.getMainLooper());
    final Runnable timeoutRunnable = new Runnable() {
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
            target.onObserverJobFinished(devices_to_observe);
        }
    };

    protected abstract void doAction(DeviceInfo di, boolean repeated);

    final Runnable redoRunnable = new Runnable() {
        @Override
        public void run() {
            for (DeviceInfo di : devices_to_observe) {
                doAction(di, true);
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

    @SuppressWarnings("SameParameterValue")
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
                target.onObserverJobFinished(devices_to_observe);
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
            target.onObserverJobFinished(devices_to_observe);
    }
}