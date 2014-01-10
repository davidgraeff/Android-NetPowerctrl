package oly.netpowerctrl.anelservice;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all scenes to query.
 */
public class DeviceQuery {
    private Collection<DeviceInfo> devices_to_observe;
    private DeviceUpdateStateOrTimeout target;
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            for (DeviceInfo di : devices_to_observe)
                target.onDeviceTimeout(di);
            target.onDeviceQueryFinished(devices_to_observe.size());
            NetpowerctrlApplication.instance.removeUpdateDeviceState(DeviceQuery.this);
        }
    };

    public DeviceQuery(Context context, DeviceUpdateStateOrTimeout target, DeviceInfo device_to_observe, boolean rangeCheck) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>();
        devices_to_observe.add(device_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.instance.addUpdateDeviceState(this);


        // Send out broadcast
        if (!DeviceSend.instance().sendQuery(context, device_to_observe.HostName, device_to_observe.SendPort, rangeCheck)) {
            // Device not in range, immediately timeout
            timeoutHandler.postDelayed(timeoutRunnable, 0);
        } else
            timeoutHandler.postDelayed(timeoutRunnable, 1200);
    }

    public DeviceQuery(Context context, DeviceUpdateStateOrTimeout target,
                       Collection<DeviceInfo> devices_to_observe, boolean queryForNewDevices, boolean rangeCheck) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>(devices_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.instance.addUpdateDeviceState(this);

        timeoutHandler.postDelayed(timeoutRunnable, 1200);

        // Send out broadcast
        if (queryForNewDevices)
            sendBroadcastQuery(context);
        else
            for (DeviceInfo di : devices_to_observe)
                DeviceSend.instance().sendQuery(context, di.HostName, di.SendPort, rangeCheck);
    }

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
            if (device_to_observe.equals(received_data)) {
                it.remove();
                target.onDeviceUpdated(received_data);
            }
        }
        if (devices_to_observe.isEmpty()) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            target.onDeviceQueryFinished(devices_to_observe.size());
            return true;
        }
        return false;
    }

    /**
     * Used to be used only from the DeviceSend class for requesting an update
     * after a command has been send
     *
     * @param context        Context
     * @param device_command A DeviceCommand object containing destination IP, Port and commands
     */
    static void sendQuery(final Context context, DeviceCommand device_command) {
        DeviceSend.instance().sendQuery(context, device_command.dest.getHostAddress(), device_command.port, false);
    }

    private static void sendBroadcastQuery(final Context context) {
        for (int port : NetpowerctrlApplication.instance.getAllSendPorts())
            DeviceSend.instance().sendQuery(context, "255.255.255.255", port, false);
    }
}
