package oly.netpowerctrl.network;

import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all devices to query.
 */
public class DeviceQuery extends DeviceObserverBase {

    public DeviceQuery(Context context, DeviceObserverResult target, Device device_to_observe) {
        super(context, target);
        int wait = addDevice(device_to_observe, false);

        if (wait == 0)
            startQuery();
    }

    public DeviceQuery(Context context, DeviceObserverResult target, Iterator<Device> devices_to_observe) {
        super(context, target);
        int wait = 0;
        while (devices_to_observe.hasNext()) {
            wait = addDevice(devices_to_observe.next(), false);
        }

        if (wait == 0)
            startQuery();
    }

    /**
     * Issues a broadcast query. If there is no response to that for all
     * configured devices, we will also do a device specific query.
     *
     * @param target The object where the result will be propagated to.
     */
    public DeviceQuery(Context context, DeviceObserverResult target) {
        super(context, target);
        List<Device> deviceList = AppData.getInstance().deviceCollection.getItems();
        int wait = 0;
        for (Device device : deviceList) {
            wait += addDevice(device, false);
        }

        broadcast = true;
        if (wait == 0)
            startQuery();
    }

    @Override
    protected void doAction(Device device, boolean repeated) {
        if (repeated)
            Log.w("DeviceObserverBase", "redo: " + device.DeviceName);

        PluginInterface pluginInterface = device.getPluginInterface();
        if (pluginInterface == null) {
            device.setNotReachableAll(App.getAppString(R.string.error_plugin_not_installed));
            // remove from list of devices to observe and notify observers
            notifyObservers(device);
            return;
        }

        for (DeviceConnection ci : device.DeviceConnections) {
            pluginInterface.requestData(ci);
        }
    }
}
