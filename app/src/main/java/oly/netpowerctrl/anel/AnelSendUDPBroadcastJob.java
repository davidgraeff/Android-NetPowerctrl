package oly.netpowerctrl.anel;

import android.util.Log;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Set;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPErrors;
import oly.netpowerctrl.utils.Logging;

/**
 * Always have to be called from the Thread of the given DeviceQuery.
 */
public class AnelSendUDPBroadcastJob {
    public static void run(DataService dataService) {
        Set<Integer> ports = dataService.connections.getAllSendPorts();

        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

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

                        //String portString = "";
                        for (int port : ports) {
                            //portString += " " + String.valueOf(port);
                            UDPErrors.sendPacketHandleErrors(App.instance,
                                    datagramSocket, broadcast, port, "wer da?\r\n".getBytes());
                        }

                        //Log.w("AnelSendUDPBroadcastJob", "Query " + portString);
                    }
                }
            }
        } catch (SocketException ex) {
            Logging.getInstance().logDetect("UDP AnelSendUDPBroadcastJob: Error while getting network interfaces");
            Log.w("sendBroadcastQuery", "Error while getting network interfaces");
            ex.printStackTrace();
        }
    }
}
