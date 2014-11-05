package oly.netpowerctrl.network;

import android.content.Context;
import android.os.Handler;

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
    private final List<Device> timeout_devices = new ArrayList<>();
    protected boolean broadcast = false;
    private onDeviceObserverResult target;

    DeviceObserverBase(Context context, onDeviceObserverResult target) {
        this.context = context;
        mainLoopHandler = new Handler(context.getMainLooper());
        setDeviceQueryResult(target);
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
                    device_to_observe.setUniqueDeviceID(device.getUniqueDeviceID());
                    device_to_observe.setStatusMessageAllConnections(null);
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
            boolean eq = device_name.equals(device.DeviceName);
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

        if (service.isNetworkReducedMode())
            service.enterFullNetworkMode(false, false);

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

        if (!device.isEnabled()) {
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_disabled));
            timeout_devices.add(device);
            AppData.getInstance().deviceCollection.updateNotReachable(context, device);
            return countWait.get();
        }

        // We explicitly allow devices without connections (udp/http). Those connections
        // may be added later by a broadcast response. Out of the same reason we allow
        // devices without a unique id (MAC address) if only the hostname is known.
        // But we do not allow a device without a unique id and without a hostname.
        if (device.getUniqueDeviceID() == null && device.DeviceConnections.isEmpty()) {
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_incomplete));
            timeout_devices.add(device);
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

        for (Device device : devices_to_observe) {
            if (device.getFirstReachableConnection() != null)
                device.setStatusMessageAllConnections(context.getString(R.string.error_timeout_device, ""));
            // Call onConfiguredDeviceUpdated to update device info.
            AppData.getInstance().deviceCollection.updateNotReachable(context, device);
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
            List<DeviceConnection> connections = new ArrayList<>(device.DeviceConnections);
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
