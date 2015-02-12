package oly.netpowerctrl.pluginservice;

import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.executables.ExecutableReachability;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onDeviceObserverResult;

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
    private boolean broadcastFirst;

    public DeviceQuery(PluginService context, onDeviceObserverResult target, Device device_to_observe) {
        super(context, target, 200, 2);
        pluginService = context;
        broadcastFirst = false;
        addDevice(context.getAppData(), device_to_observe);
        start();
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
        broadcastFirst = broadcast;

        while (devices_to_observe.hasNext()) {
            Device device = devices_to_observe.next();
            addDevice(context.getAppData(), device);
        }

        start();
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    @Override
    protected boolean runStarted() {
        if (broadcastFirst) {
            Log.w(TAG, "Broadcast Query");
            pluginService.requestDataAll(this);
            handler.sendEmptyMessageDelayed(MSG_REPEAT, repeatTimeout);
            return false;
        }
        return true;
    }

    private void lookupIP(DeviceConnection connection) {
        if (!connection.needResolveName()) return;

        try {
            connection.lookupIPs();
        } catch (UnknownHostException e) {
            connection.device.setStatusMessage(connection, e.getLocalizedMessage(), ExecutableReachability.NotReachable);
        }
    }

    @Override
    protected void doAction(Device device, int remainingRepeats) {
        Log.w(TAG, "Query " + device.getDeviceName());

        AbstractBasePlugin abstractBasePlugin = (AbstractBasePlugin) device.getPluginInterface();
        // First try to find the not assigned plugin
        if (abstractBasePlugin == null) {
            abstractBasePlugin = PluginService.getService().getPlugin(device.pluginID);
            device.setPluginInterface(abstractBasePlugin);
        }
        if (abstractBasePlugin == null) {
            device.lockDevice();
            device.setStatusMessageAllConnections(App.getAppString(R.string.error_plugin_not_installed));
            device.releaseDevice();
            // remove from list of devices to observe and notify observers
            //notifyObserversInternal(device);
            return;
        }

        if (!abstractBasePlugin.isStarted()) {
            device.lockDevice();
            device.setStatusMessageAllConnections(App.getAppString(R.string.device_energysave_mode));
            device.releaseDevice();
            // remove from list of devices to observe and notify observers
            notifyObserversInternal(device);
            return;
        }

        // if this is the first request, we only use the first available connection.
        // If there are no available, we use all.
        List<Integer> deviceCollectionList = new ArrayList<>();

        device.lockDevice();
        int i = 0;
        for (DeviceConnection ci : device.getDeviceConnections()) {
            lookupIP(ci);
            if (ci.reachableState() != ExecutableReachability.NotReachable) {
                deviceCollectionList.add(i);
                break;
            }
            ++i;
        }

        // If this is a repeated request or if no single device connections
        // was available before, we use all device connections.
        if (deviceCollectionList.size() == 0) {
            i = 0;
            for (DeviceConnection ci : device.getDeviceConnections()) {
                lookupIP(ci);
                deviceCollectionList.add(i);
                ++i;
            }
        }
        device.releaseDevice();

        for (int deviceConnectionID : deviceCollectionList)
            abstractBasePlugin.requestData(device, deviceConnectionID);
    }
}
