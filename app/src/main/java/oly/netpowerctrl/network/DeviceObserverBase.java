package oly.netpowerctrl.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
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
    final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            finishWithTimeouts();
        }
    };
    final Runnable redoRunnable = new Runnable() {
        @Override
        public void run() {
            retryRemainingDevices();
        }
    };
    private final List<Device> devices_to_observe = new ArrayList<>();
    private final List<Device> timeout_devices = new ArrayList<>();
    protected boolean broadcast = false;
    private boolean timeoutRepeat = false;
    private onDeviceObserverResult target;

    DeviceObserverBase(Context context, onDeviceObserverResult target) {
        this.context = context;
        mainLoopHandler = new Handler(context.getMainLooper());
        setDeviceQueryResult(target);
    }

    private void retryRemainingDevices() {
        List<Device> deviceList = new ArrayList<>(devices_to_observe);
        for (Device device : deviceList) {
            doAction(device, true);
        }
    }

    public void setDeviceQueryResult(onDeviceObserverResult target) {
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
            if (device_to_observe.getUniqueDeviceID() == null) {
                if (device_to_observe.hasAddress(device.getHostnameIPs(false), false)) {
                    device_to_observe.lockDevice();
                    device_to_observe.setUniqueDeviceID(device.getUniqueDeviceID());
                    device_to_observe.releaseDevice();
                    device_to_observe.clearStatusMessageAllConnections();
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
        return devices_to_observe.isEmpty();
    }

    public boolean notifyObservers(String device_name) {
        Iterator<Device> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            Device device = it.next();
            boolean eq = device_name.equals(device.getDeviceName());
            if (eq) {
                it.remove();
                if (target != null)
                    target.onObserverDeviceUpdated(device);
                break;
            }
        }
        return devices_to_observe.isEmpty();
    }

    public void clearDevicesToObserve() {
        devices_to_observe.clear();
        timeout_devices.clear();
    }

    protected boolean isEmpty() {
        return devices_to_observe.isEmpty();
    }

    public void startQuery() {
        ListenService service = ListenService.getService();
        if ((isEmpty() && !broadcast) || service == null) {
            if (target != null)
                target.onObserverJobFinished(timeout_devices);
            return;
        }

        // Register on main application object to receive device updates
        AppData.getInstance().addUpdateDeviceState(this);

        // We do not repeat the broadcast as often as individual requests
        if (!broadcast) {
            mainLoopHandler.postDelayed(redoRunnable, 300);
            mainLoopHandler.postDelayed(redoRunnable, 1200);
        }
        mainLoopHandler.postDelayed(redoRunnable, 600);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);

        if (broadcast) {
            service.wakeupAllDevices(false);
            service.sendBroadcastQuery();
        } else {
            // Send out broadcast
            List<Device> deviceList = new ArrayList<>(devices_to_observe);
            for (Device device : deviceList) {
                service.wakeupPlugin(device);
                doAction(device, true);
            }
        }

        // one time only, flag reset
        broadcast = false;
    }

    @SuppressWarnings("SameParameterValue")
    public int addDevice(final Device device, boolean resetTimeout) {

        if (!device.isEnabled()) {
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_disabled));
            timeout_devices.add(device);
            AppData.getInstance().updateExistingDevice(device);
            return countWait.get();
        }

        // We explicitly allow devices without connections (udp/http). Those connections
        // may be added later by a broadcast response. Out of the same reason we allow
        // devices without a unique id (MAC address) if only the hostname is known.
        // But we do not allow a device without a unique id and without a hostname.
        if (device.getUniqueDeviceID() == null && device.hasNoConnections()) {
            device.lockDevice();
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_incomplete));
            device.releaseDevice();
            timeout_devices.add(device);
            AppData.getInstance().updateExistingDevice(device);
            return countWait.get();
        }

        // If the device has connections we have to check now for hostname->ip resolving.
        // This can only be done in another thread.
        boolean needResolve = false;
        device.lockDevice();
        for (final DeviceConnection connection : device.getDeviceConnections()) {
            if (connection.needResolveName()) {
                resetTimeout = true; // we need more time
                needResolve = true;
                break;
            }
        }
        device.releaseDevice();

        if (resetTimeout) {
            repeatTimeout(1500);
        }

        if (!needResolve) {
            devices_to_observe.add(device);
            return countWait.get();
        } else {
            timeoutRepeat = true;
            new Thread(new ResolveHostnameRunnable(device, this)).start();
            return countWait.incrementAndGet();
        }
    }

    private void finishedLookup(Device device) {
        devices_to_observe.add(device);
        if (countWait.decrementAndGet() == 0) {
            startQuery();
        }
    }

    private void repeatTimeout(int time_in_ms) {
        mainLoopHandler.removeCallbacks(timeoutRunnable);
        mainLoopHandler.postDelayed(timeoutRunnable, time_in_ms);
    }

    /**
     * Called right before this object is removed from the Application list
     * of DeviceQueries because the listener service has been shutdown. All
     * remaining device queries of this object have to timeout now.
     */
    public void finishWithTimeouts() {
        mainLoopHandler.removeCallbacks(timeoutRunnable);
        mainLoopHandler.removeCallbacks(redoRunnable);

        // Remove update listener manually to be sure. Normally this is done by the notify.. method caller by
        // returning false if all devices responded. We have to remove the listener before calling
        // onObserverJobFinished to not get into an endless loop!
        AppData.getInstance().removeUpdateDeviceState(DeviceObserverBase.this);

        // Wait for all ResolveHostnameRunnable threads, max 200ms (we already waited 1,5sec longer)
        if (countWait.get() != 0) {
            if (timeoutRepeat) {
                timeoutRepeat = false;
                repeatTimeout(200);
                return;
            } else {
                Log.w("finishWithTimeouts", "DNS resolve to long: " + String.valueOf(countWait.get()));
            }
        }

        // We have to work with a copy of devices_to_observe here, because ResolveHostnameRunnable may
        // return while we are in this list!
        List<Device> deviceList = new ArrayList<>(devices_to_observe);
        for (Device device : deviceList) {
            if (device.getFirstReachableConnection() != null)
                device.setStatusMessageAllConnections(context.getString(R.string.error_timeout_device, ""));
            // Call onConfiguredDeviceUpdated to update device info.
            AppData.getInstance().updateExistingDevice(device);
            timeout_devices.add(device);
        }
        devices_to_observe.clear();

        if (target != null)
            target.onObserverJobFinished(timeout_devices);
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
            device.lockDevice();
            List<DeviceConnection> connections = new ArrayList<>(device.getDeviceConnections());
            device.releaseDevice();
            for (final DeviceConnection connection : connections) {
                if (connection.needResolveName()) {
                    try {
                        connection.lookupIPs();
                    } catch (UnknownHostException e) {
                        connection.device.setStatusMessage(connection, e.getLocalizedMessage(), true);
                    }
                }
            }

            final DeviceObserverBase deviceObserverBase = deviceObserverBaseWeakReference.get();
            if (deviceObserverBase == null)
                return;

            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    deviceObserverBase.finishedLookup(device);
                }
            });
        }
    }
}
