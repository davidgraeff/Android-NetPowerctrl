package oly.netpowerctrl.network;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.devices.DeviceInfo;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all scenes to query.
 */
public class DeviceQuery extends DeviceObserverBase {

    public DeviceQuery(DeviceObserverResult target, DeviceInfo device_to_observe) {
        setDeviceQueryResult(target);
        devices_to_observe = new ArrayList<>();
        devices_to_observe.add(device_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        doAction(device_to_observe, false);
        mainLoopHandler.postDelayed(redoRunnable, 300);
        mainLoopHandler.postDelayed(redoRunnable, 600);
        mainLoopHandler.postDelayed(redoRunnable, 1200);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);
    }

    public DeviceQuery(DeviceObserverResult target, Collection<DeviceInfo> devices_to_observe) {
        setDeviceQueryResult(target);
        this.devices_to_observe = new ArrayList<>(devices_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        mainLoopHandler.postDelayed(redoRunnable, 300);
        mainLoopHandler.postDelayed(redoRunnable, 600);
        mainLoopHandler.postDelayed(redoRunnable, 1200);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);

        // Send out broadcast
        for (DeviceInfo di : devices_to_observe)
            doAction(di, false);
    }

    /**
     * Issues a broadcast query. If there is no response to that for all
     * configured devices, we will also do a device specific query.
     *
     * @param target The object where the result will be propagated to.
     */
    public DeviceQuery(DeviceObserverResult target) {
        setDeviceQueryResult(target);
        this.devices_to_observe = new ArrayList<>(NetpowerctrlApplication.getDataController().deviceCollection.devices);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

        mainLoopHandler.postDelayed(redoRunnable, 600);
        mainLoopHandler.postDelayed(timeoutRunnable, 1500);

        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;
        service.sendBroadcastQuery();
    }

    //    if (deviceName.equalsByUniqueID(device.DeviceName)) {
//        test_state = TestStates.TEST_INIT;
//    }

    @Override
    protected void doAction(DeviceInfo di, boolean repeated) {
        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;

        if (repeated)
            Log.w("DeviceObserverBase", "redo: " + di.DeviceName);
        PluginInterface remote = di.getPluginInterface(service);

        if (remote != null) {
            remote.requestData(di);
        } else {
            di.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_plugin_not_installed));
        }
    }
}
