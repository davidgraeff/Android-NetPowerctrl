package oly.netpowerctrl.network;

import android.content.Context;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;

/**
 * This base class is used by the device query and device-resend-command class.
 * It will be registered on the main application to receive device updates and
 * provide the result of a query/a sending action to the DeviceQueryResult object.
 */
public abstract class DeviceObserverBase {
    protected final Handler mainLoopHandler;
    protected final Context context;
    protected final AtomicInteger countWait = new AtomicInteger();
    private final List<Device> devices_to_observe = new ArrayList<>();
    final Runnable redoRunnable = new Runnable() {
        @Override
        public void run() {
            List<Device> deviceList = new ArrayList<>(devices_to_observe);
            for (Device device : deviceList) {
                doAction(device, true);
            }
        }
    };
    final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            for (Device device : devices_to_observe) {
                if (device.getFirstReachableConnection() != null)
                    device.setNotReachableAll(context.getString(R.string.error_timeout_device, ""));
                // Call onConfiguredDeviceUpdated to update device info.
                AppData.getInstance().deviceCollection.updateNotReachable(context, device);
            }
            devices_to_observe.clear();

            checkIfDone();
            // Remove update listener manually. Normally this is done by the notify.. method caller.
            AppData.getInstance().removeUpdateDeviceState(DeviceObserverBase.this);
        }
    };
    private final List<Device> devices_to_observe_not_filtered = new ArrayList<>();
    protected boolean broadcast = false;
    private DeviceObserverResult target;

    DeviceObserverBase(Context context, DeviceObserverResult target) {
        this.context = context;
        mainLoopHandler = new Handler(context.getMainLooper());
        setDeviceQueryResult(target);
    }

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
            // Special case: No unique id. IPs are compared instead
            // and the unique id is copied from the network device.
            if (device_to_observe.UniqueDeviceID == null) {
                if (device_to_observe.hasAddress(device.getHostnameIPs())) {
                    device_to_observe.UniqueDeviceID = device.UniqueDeviceID;
                    device_to_observe.setNotReachableAll(null);
                    it.remove();
                    if (target != null)
                        target.onObserverDeviceUpdated(device);
                    break;
                }
            } else if (device_to_observe.equalsByUniqueID(device)) {
                it.remove();
                if (target != null)
                    target.onObserverDeviceUpdated(device);
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
                    target.onObserverDeviceUpdated(device);
                break;
            }
        }
        return checkIfDone();
    }

    public void clearDevicesToObserve() {
        devices_to_observe.clear();
        devices_to_observe_not_filtered.clear();
    }

    protected boolean isEmpty() {
        return devices_to_observe.isEmpty();
    }

    public void startQuery() {
        ListenService service = ListenService.getService();
        if ((isEmpty() && !broadcast) || service == null) {
            if (target != null)
                target.onObserverJobFinished(devices_to_observe_not_filtered);
            return;
        }

        // Register on main application object to receive device updates
        AppData.getInstance().addUpdateDeviceState(this);

        // We do not repeat the broadcast as much as individual requests
        if (!broadcast) {
            mainLoopHandler.postDelayed(redoRunnable, 300);
            mainLoopHandler.postDelayed(redoRunnable, 1200);
        }
        mainLoopHandler.postDelayed(redoRunnable, 600);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);

        if (broadcast) {
            service.sendBroadcastQuery();
        } else {
            // Send out broadcast
            List<Device> deviceList = new ArrayList<>(devices_to_observe);
            for (Device device : deviceList) {
                doAction(device, true);
            }
        }

        // one time only, flag reset
        broadcast = false;
    }

    @SuppressWarnings("SameParameterValue")
    public int addDevice(final Device device, boolean resetTimeout) {
        devices_to_observe_not_filtered.add(device);

        if (!device.isEnabled()) {
            device.setNotReachableAll(context.getString(R.string.error_device_disabled));
            AppData.getInstance().deviceCollection.updateNotReachable(context, device);
            return countWait.get();
        }

        // We explicitly allow devices without connections (udp/http). Those connections
        // may be added later by a broadcast response. Out of the same reason we allow
        // devices without a unique id (MAC address) if only the hostname is known.
        // But we do not allow a device without a unique id and without a hostname.
        if (device.UniqueDeviceID == null && device.DeviceConnections.isEmpty()) {
            device.setNotReachableAll(context.getString(R.string.error_device_incomplete));
            AppData.getInstance().deviceCollection.updateNotReachable(context, device);
            return countWait.get();
        }

        // If the device has connections we have to check now for hostname->ip resolving.
        // This can only be done in another thread.
        boolean needResolve = false;
        for (final DeviceConnection connection : device.DeviceConnections) {
            if (connection.needResolveName()) {
                resetTimeout = true; // we need more time
                needResolve = true;
                break;
            }
        }

        if (resetTimeout) {
            mainLoopHandler.removeCallbacks(timeoutRunnable);
            mainLoopHandler.postDelayed(timeoutRunnable, 1500);
        }

        if (!needResolve) {
            devices_to_observe.add(device);
            return countWait.get();
        } else {
            new Thread(new ResolveHostnameRunnable(device, this)).start();
            return countWait.incrementAndGet();
        }
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
            di.setNotReachableAll(context.getString(R.string.error_timeout_device, ""));
        }
        if (target != null)
            target.onObserverJobFinished(devices_to_observe);
    }

    private static class ResolveHostnameRunnable implements Runnable {
        private final Device device;
        private final WeakReference<DeviceObserverBase> deviceObserverBaseWeakReference;

        public ResolveHostnameRunnable(Device device, DeviceObserverBase deviceObserverBase) {
            this.device = device;
            this.deviceObserverBaseWeakReference = new WeakReference<>(deviceObserverBase);
        }

        @Override
        public void run() {
            for (final DeviceConnection connection : device.DeviceConnections) {
                if (connection.needResolveName()) {
                    connection.getHostnameIPs();
                }
            }

            final DeviceObserverBase deviceObserverBase = deviceObserverBaseWeakReference.get();
            if (deviceObserverBase == null)
                return;

            deviceObserverBase.devices_to_observe.add(device);
            if (deviceObserverBase.countWait.decrementAndGet() == 0) {
                App.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        deviceObserverBase.startQuery();
                    }
                });
            }
        }
    }
}
