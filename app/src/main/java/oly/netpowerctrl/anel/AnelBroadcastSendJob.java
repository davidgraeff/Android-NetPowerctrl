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
import java.util.Set;

import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.utils.Logging;

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
        UDPSending udpSending = udpSendingReference.get();
        if (udpSending == null) {
            Logging.getInstance().logDetect("UDP AnelBroadcastSendJob: No udp Sending!");
            return;
        }
        Set<Integer> ports = PluginService.getService().getAppData().getAllSendPorts();

        DatagramSocket datagramSocket = udpSending.datagramSocket;
        boolean logDetect = Logging.getInstance().mLogDetect;

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

                        if (logDetect) {
                            String portsString = "";
                            for (Integer port : ports) portsString += String.valueOf(port) + " ";
                            Logging.getInstance().logDetect("UDP Broadcast on " + broadcast.toString() + " Ports: " + portsString);
                        }

                        for (int port : ports)
                            sendPacket(App.instance, datagramSocket, broadcast, port, "wer da?\r\n".getBytes());
                    }
                }
            }
        } catch (SocketException ex) {
            Logging.getInstance().logDetect("UDP AnelBroadcastSendJob: Error while getting network interfaces");
            Log.w("sendBroadcastQuery", "Error while getting network interfaces");
            ex.printStackTrace();
        }
    }
}
