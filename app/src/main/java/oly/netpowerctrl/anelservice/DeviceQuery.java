package oly.netpowerctrl.anelservice;

import android.content.Context;
import android.os.Handler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.ShowToast;

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
            freeSelf();
        }
    };

    public DeviceQuery(Context context, DeviceUpdateStateOrTimeout target, DeviceInfo device_to_observe) {
        this.target = target;
        this.devices_to_observe = new ArrayList<DeviceInfo>();
        devices_to_observe.add(device_to_observe);

        // Register on main application object to receive device updates
        NetpowerctrlApplication.instance.addUpdateDeviceState(this);

        timeoutHandler.postDelayed(timeoutRunnable, 1200);
        // Send out broadcast
        sendQuery(context, device_to_observe.HostName, device_to_observe.SendPort);
    }

    public DeviceQuery(Context context, DeviceUpdateStateOrTimeout target,
                       Collection<DeviceInfo> devices_to_observe, boolean queryForNewDevices) {
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
                sendQuery(context, di.HostName, di.SendPort);
    }

    public void notifyAndRemove(DeviceInfo received_data) {
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
            freeSelf();
        }
    }

    private void freeSelf() {
        target.onDeviceQueryFinished(devices_to_observe.size());
        NetpowerctrlApplication.instance.removeUpdateDeviceState(this);
    }

    /**
     * Used to be used only from the DeviceSend class for requesting an update
     * after a command has been send
     *
     * @param context
     * @param device_command
     */
    static void sendQuery(final Context context, DeviceCommand device_command) {
        sendQuery(context, device_command.dest.getHostAddress(), device_command.port);
    }

    private static void sendQuery(final Context context, final String hostname, final int port) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String messageStr = "wer da?\r\n";
                    DatagramSocket s = new DatagramSocket();
                    s.setBroadcast(true);
                    InetAddress host = InetAddress.getByName(hostname);
                    int msg_length = messageStr.length();
                    byte[] message = messageStr.getBytes();
                    DatagramPacket p = new DatagramPacket(message, msg_length, host, port);
                    s.send(p);
                    s.close();
                } catch (final Exception e) {
                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_sending_inquiry) + ": " + e.getMessage());
                }
            }
        }).start();
    }

    private static void sendBroadcastQuery(final Context context) {
        for (int port : NetpowerctrlApplication.instance.getAllSendPorts())
            sendQuery(context, "255.255.255.255", port);
    }
}
