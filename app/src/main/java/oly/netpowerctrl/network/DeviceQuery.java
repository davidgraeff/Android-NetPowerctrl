package oly.netpowerctrl.network;

import android.util.Log;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.executables.ExecutableReachability;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.AbstractBasePlugin;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all devices to query.
 */
public class DeviceQuery extends DeviceObserverBase {
    @SuppressWarnings("unused")
    private static final String TAG = "DeviceQuery";
    private final PluginService pluginService;
    private ResolveThread resolveThread = new ResolveThread();

    public DeviceQuery(PluginService context, onDeviceObserverResult target, Device device_to_observe) {
        super(context, target, 200, 2);
        pluginService = context;
//        Log.w(TAG, "DeviceQuery");
        addDevice(context.getAppData(), device_to_observe);
        startQuery();
    }

    /**
     * @param context            A context
     * @param target             The object where the result will be propagated to.
     * @param devices_to_observe An iterator to the list of devices to observe
     * @param broadcast          Issues a broadcast query. If there is no response to that for all
     *                           configured devices, we will also do a device specific query.
     */
    public DeviceQuery(PluginService context, onDeviceObserverResult target, Iterator<Device> devices_to_observe, boolean broadcast) {
        super(context, target, 200, broadcast ? 3 : 2);
        pluginService = context;
        this.broadcast = broadcast;
//        Log.w(TAG, "DeviceQuery");
        while (devices_to_observe.hasNext()) {
            addDevice(context.getAppData(), devices_to_observe.next());
        }

        startQuery();
    }

    @Override
    public void startQuery() {
        resolveThread.start();
        super.startQuery();
    }

    @Override
    protected void doAction(Device device, int remainingRepeats) {
        if (broadcast) {
            broadcast = false;
            // Send out broadcast
            pluginService.wakeupAllDevices();
            pluginService.requestDataAll();
            return;
        }

        Log.w(TAG, "redo: " + device.getDeviceName());

        pluginService.wakeupPlugin(device);

        // If the device has connections we have to check now for hostname->ip resolving.
        // This can only be done in another thread.
        device.lockDevice();
        for (final DeviceConnection connection : device.getDeviceConnections()) {
            if (connection.needResolveName()) {
                resolveThread.q.add(connection);
                return;
            }
        }
        device.releaseDevice();

        AbstractBasePlugin abstractBasePlugin = (AbstractBasePlugin) device.getPluginInterface();
        // First try to find the not assigned plugin
        if (abstractBasePlugin == null) {
            abstractBasePlugin = PluginService.getService().getPlugin(device.pluginID);
            device.setPluginInterface(abstractBasePlugin);
        }
        if (abstractBasePlugin == null) {
            device.setStatusMessageAllConnections(App.getAppString(R.string.error_plugin_not_installed));
            // remove from list of devices to observe and notify observers
            //notifyObserversInternal(device);
            return;
        }

        if (abstractBasePlugin.isNetworkReducedState()) {
            device.setStatusMessageAllConnections(App.getAppString(R.string.device_energysave_mode));
            // remove from list of devices to observe and notify observers
            notifyObserversInternal(device);
            return;
        }

        boolean requestAll = true;
        // if this is the first request, we only use the first available connection.
        // If there are no available, we use all.
        int i = 0;
        device.lockDevice();
        for (DeviceConnection ci : device.getDeviceConnections()) {
            if (ci.reachableState() != ExecutableReachability.NotReachable) {
                requestAll = false;
                abstractBasePlugin.requestData(device, i);
                break;
            }
            ++i;
        }
        device.releaseDevice();

        // If this is a repeated request or if no single device connections
        // was available before, we use all device connections.
        if (requestAll) {
            device.lockDevice();
            int s = device.getDeviceConnections().size();
            device.releaseDevice();
            for (i = 0; i < s; ++i) {
                abstractBasePlugin.requestData(device, i);
            }
        }

    }

    class ResolveThread extends Thread {
        public final LinkedBlockingQueue<DeviceConnection> q = new LinkedBlockingQueue<>();

        public ResolveThread() {
            super("ResolveThread");
        }

        @Override
        public void run() {
            try {
                DeviceConnection connection = q.take();

                if (connection.needResolveName()) {
                    try {
                        connection.lookupIPs();
                    } catch (UnknownHostException e) {
                        connection.device.setStatusMessage(connection, e.getLocalizedMessage(), true);
                    }
                }

            } catch (InterruptedException ignored) {
            }
        }
    }
}
