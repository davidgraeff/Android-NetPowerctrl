package oly.netpowerctrl.network;

import android.os.Handler;

import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.Device;

/**
 * This base class is used by the device query and device-resend-command class.
 * It will be registered on the main application to receive device updates and
 * provide the result of a query/a sending action to the DeviceQueryResult object.
 */
public abstract class DeviceObserverBase {
    final Handler mainLoopHandler = new Handler(NetpowerctrlApplication.instance.getMainLooper());
    List<Device> devices_to_observe;
    final Runnable redoRunnable = new Runnable() {
        @Override
        public void run() {
            for (Device device : devices_to_observe) {
                doAction(device, true);
            }
        }
    };
    private DeviceObserverResult target;
    final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            //Remove update listener
            NetpowerctrlApplication.getDataController().removeUpdateDeviceState(DeviceObserverBase.this);

            for (Device device : devices_to_observe) {
                if (device.getFirstReachableConnection() != null)
                    device.setNotReachableAll(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
                // Call onDeviceUpdated to update device info.
                NetpowerctrlApplication.getDataController().deviceCollection.updateNotReachable(device);
            }

            // Update status observer
            if (target != null) {
                target.onObserverJobFinished(devices_to_observe);
            }
        }
    };

    public void setDeviceQueryResult(DeviceObserverResult target) {
        this.target = target;
    }

    protected abstract void doAction(Device device, boolean repeated);

    /**
     * Return true if all devices responded and this DeviceQuery object
     * have to be removed.
     *
     * @param device The DeviceInfo object all observes should be notified of.
     */
    public boolean notifyObservers(Device device) {
        Iterator<Device> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            Device device_to_observe = it.next();
            if (device_to_observe.equalsByUniqueID(device)) {
                it.remove();
                if (target != null)
                    target.onDeviceUpdated(device);
                break;
            }
        }
        return checkIfDone();
    }


    public boolean notifyObservers(String device_name) {
        Iterator<Device> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            Device device = it.next();
            boolean eq = device_name.equals(device.DeviceName);
            if (eq) {
                it.remove();
                if (target != null)
                    target.onDeviceUpdated(device);
                break;
            }
        }
        return checkIfDone();
    }

    @SuppressWarnings("SameParameterValue")
    public void addDevice(Device device, boolean resetTimeout) {
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
        for (Device di : devices_to_observe) {
            di.setNotReachableAll(NetpowerctrlApplication.instance.getString(R.string.error_timeout_device, ""));
        }
        if (target != null)
            target.onObserverJobFinished(devices_to_observe);
    }
}
