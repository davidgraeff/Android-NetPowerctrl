package oly.netpowerctrl.anel;

import android.content.Context;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPSending;

/**
 * A DeviceSend.Job that provide broadcast sending to anel devices.
 */
public class AnelBroadcastSendJob implements UDPSending.Job {
    final private WeakReference<UDPSending> udpSendingReference;

    public AnelBroadcastSendJob(UDPSending udpSending) {
        this.udpSendingReference = new WeakReference<>(udpSending);
    }

    private void sendPacket(Context context, DatagramSocket datagramSocket, InetAddress ip, int SendPort, byte[] message) {
        try {
            datagramSocket.setBroadcast(true);
            datagramSocket.send(new DatagramPacket(message, message.length, ip, SendPort));
            //Log.w("AnelBroadcastSendJob",ip.getHostAddress());
        } catch (final SocketException e) {
            if (e.getMessage().contains("ENETUNREACH"))
                UDPSending.onError(context, UDPSending.NETWORK_UNREACHABLE, ip.getHostAddress(), SendPort, e);
            else {
                UDPSending.onError(context, UDPSending.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            UDPSending.onError(context, UDPSending.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
        }
    }

    @Override
    public void process() {
        Context context = ListenService.getService();
        if (context == null)
            return;

        Set<Integer> ports = AppData.getInstance().getAllSendPorts();
        boolean foundBroadcastAddresses = false;

        UDPSending udpSending = udpSendingReference.get();
        if (udpSending == null)
            return;
        DatagramSocket datagramSocket = udpSending.datagramSocket;

        Enumeration list;
        try {
            list = NetworkInterface.getNetworkInterfaces();

            while (list.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) list.nextElement();

                if (networkInterface == null) continue;

                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        //System.out.println("Found address: " + address);
                        if (address == null) continue;
                        InetAddress broadcast = address.getBroadcast();
                        if (broadcast == null) continue;
                        for (int port : ports)
                            sendPacket(context, datagramSocket, broadcast, port, "wer da?\r\n".getBytes());
                        foundBroadcastAddresses = true;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.w("sendBroadcastQuery", "Error while getting network interfaces");
            ex.printStackTrace();
        }

        if (!foundBroadcastAddresses) {
            // Broadcast not allowed on this network. Show hint to user
//                Toast.makeText(NetpowerctrlApplication.instance,
//                        NetpowerctrlApplication.getAppString(R.string.devices_no_new_on_network),
//                        Toast.LENGTH_SHORT).show();

            // Query all existing anel devices directly

            List<Device> devices = AppData.getInstance().deviceCollection.getItems();
            for (Device device : devices) {
                if (device.pluginID.equals(AnelPlugin.PLUGIN_ID)) {
                    AnelPlugin pluginInterface = (AnelPlugin) device.getPluginInterface();
                    if (pluginInterface == null) {
                        device.setStatusMessageAllConnections(App.getAppString(R.string.error_plugin_not_installed));
                        continue;
                    }
                    device.lockDevice();
                    int s = device.getDeviceConnections().size();
                    device.releaseDevice();
                    for (int i = 0; i < s; ++i) {
                        pluginInterface.requestData(device, i);
                    }
                }
            }
        }
    }
}
