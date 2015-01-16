package oly.netpowerctrl.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.DeviceObserverBase;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Created by david on 17.12.14.
 */
public class SendAndObserve extends DeviceObserverBase {
    @SuppressWarnings("unused")
    private static final String TAG = "SendAndObserveJob";
    final DeviceConnection ci;
    final List<byte[]> messages = new ArrayList<>();
    final int errorID;
    InetAddress ip = null;
    private DatagramSocket datagramSocket;

    public SendAndObserve(@NonNull PluginService service,
                          @NonNull DeviceConnection ci, byte[] message, int errorID) {
        super(service, null, 200, 3);
//            Log.w(TAG, "SendAndObserveJob");
        this.messages.add(message);
        this.errorID = errorID;
        this.ci = ci;
        clearDevicesToObserve();
        addDevice(service.getAppData(), ci.getDevice());

        start();
    }

    public SendAndObserve(@NonNull PluginService service,
                          @NonNull DeviceConnection ci, byte[] message, byte[] message2, int errorID) {
        super(service, null, 200, 3);
//            Log.w(TAG, "SendAndObserveJob");
        this.messages.add(message);
        this.messages.add(message2);
        this.errorID = errorID;
        this.ci = ci;
        clearDevicesToObserve();
        addDevice(service.getAppData(), ci.getDevice());

        start();
    }

    @Override
    protected boolean runStarted() {
        // Get IP
        try {
            if (ip == null) {
                ip = InetAddress.getByName(ci.getDestinationHost());
            }
        } catch (final UnknownHostException e) {
            UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNKNOWN_HOSTNAME, ci.getDestinationHost(), ci.getDestinationPort(), e);
            finishWithTimeouts();
            return false;
        }

        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(false);
        } catch (SocketException e) {
            UDPErrors.onError(App.instance, UDPErrors.INQUERY_REQUEST, ci.getDestinationHost(), ci.getDestinationPort(), e);
            finishWithTimeouts();
            return false;
        }

        return true;
    }

    @Override
    protected void doAction(@Nullable Device device, int remainingRepeats) {
        for (byte[] message : messages) {
            try {
                datagramSocket.send(new DatagramPacket(message, message.length, ip, ci.getDestinationPort()));

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPErrors.onError(context, UDPErrors.NETWORK_UNREACHABLE, ip.getHostAddress(), ci.getDestinationPort(), e);
                else {
                    UDPErrors.onError(context, UDPErrors.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), ci.getDestinationPort(), e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPErrors.onError(context, UDPErrors.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), ci.getDestinationPort(), e);
            }
        }
    }
}
