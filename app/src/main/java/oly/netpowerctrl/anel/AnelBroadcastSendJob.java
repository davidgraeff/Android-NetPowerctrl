package oly.netpowerctrl.anel;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Set;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.network.DeviceSend;

/**
 * Created by david on 21.02.14.
 */
public class AnelBroadcastSendJob implements DeviceSend.Job {
    private void sendPacket(DeviceSend deviceSend, InetAddress ip, int SendPort, byte[] message) {
        try {
            deviceSend.datagramSocket.setBroadcast(true);
            deviceSend.datagramSocket.send(new DatagramPacket(message, message.length, ip, SendPort));
            //Log.w("AnelBroadcastSendJob",ip.getHostAddress());
        } catch (final SocketException e) {
            if (e.getMessage().contains("ENETUNREACH"))
                DeviceSend.onError(DeviceSend.NETWORK_UNREACHABLE, ip.getHostAddress(), SendPort, e);
            else {
                DeviceSend.onError(DeviceSend.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            DeviceSend.onError(DeviceSend.INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
        }
    }

    @Override
    public void process(DeviceSend deviceSend) {
        Set<Integer> ports = NetpowerctrlApplication.getDataController().getAllSendPorts();
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
                            sendPacket(deviceSend, broadcast, port, "wer da?\r\n".getBytes());
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
//                        NetpowerctrlApplication.instance.getString(R.string.devices_no_new_on_network),
//                        Toast.LENGTH_SHORT).show();

            // Query all existing devices directly
            ArrayList<DeviceInfo> devices = NetpowerctrlApplication.getDataController().configuredDevices;
            for (DeviceInfo di : devices) {
                deviceSend.addSendJob(di, "wer da?\r\n".getBytes(), DeviceSend.INQUERY_REQUEST, false);
            }
        }
    }
}
