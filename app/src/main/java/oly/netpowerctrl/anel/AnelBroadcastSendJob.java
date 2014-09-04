package oly.netpowerctrl.anel;

import android.content.Context;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPSending;

/**
 * A DeviceSend.Job that provide broadcast sending to anel devices.
 */
public class AnelBroadcastSendJob implements UDPSending.Job {
    private void sendPacket(Context context, UDPSending udpSending, InetAddress ip, int SendPort, byte[] message) {
        try {
            udpSending.datagramSocket.setBroadcast(true);
            udpSending.datagramSocket.send(new DatagramPacket(message, message.length, ip, SendPort));
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
    public void process(UDPSending UDPSending) {
        Context context = ListenService.getService();
        if (context == null)
            return;

        Set<Integer> ports = AppData.getInstance().getAllSendPorts();
        boolean foundBroadcastAddresses = false;

        Enumeration list;
        try {
            list = NetworkInterface.getNetworkInterfaces();

            while (list.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) list.nextElement();

                if (iface == null) continue;

                if (!iface.isLoopback() && iface.isUp()) {
                    for (InterfaceAddress address : iface.getInterfaceAddresses()) {
                        //System.out.println("Found address: " + address);
                        if (address == null) continue;
                        InetAddress broadcast = address.getBroadcast();
                        if (broadcast == null) continue;
                        for (int port : ports)
                            sendPacket(context, UDPSending, broadcast, port, "wer da?\r\n".getBytes());
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
                    PluginInterface i = device.getPluginInterface();
                    if (i == null) {
                        device.setNotReachableAll(App.getAppString(R.string.error_plugin_not_installed));
                        continue;
                    }
                    for (DeviceConnection ci : device.DeviceConnections) {
                        i.requestData(ci);
                    }
                }
            }
        }
    }
}
