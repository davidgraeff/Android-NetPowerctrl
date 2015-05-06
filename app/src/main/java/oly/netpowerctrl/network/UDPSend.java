package oly.netpowerctrl.network;

import android.support.annotation.NonNull;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.Logging;

/**
 * For sending udp packets
 */
public class UDPSend extends Thread {
    @SuppressWarnings("unused")
    private static final String TAG = "SendAndObserveJob";
    private String host;
    private int destPort;
    private Set<Integer> ports;
    private List<byte[]> messages = new ArrayList<>();
    private int errorID;
    private InetAddress ip = null;
    private boolean broadcast = false;

    private UDPSend(byte[] message, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.errorID = errorID;
    }

    public UDPSend(@NonNull IOConnection ioConnection, byte[] message, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.errorID = errorID;
        this.host = ioConnection.getDestinationHost();
        this.destPort = ioConnection.getDestinationPort();
        start();
    }

    public UDPSend(@NonNull IOConnection ioConnection, byte[] message, byte[] message2, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.messages.add(message2);
        this.errorID = errorID;
        this.host = ioConnection.getDestinationHost();
        this.destPort = ioConnection.getDestinationPort();
        start();
    }

    public static void createBroadcast(DataService dataService, byte[] message, int errorID) {
        UDPSend udpSend = new UDPSend(message, errorID);
        udpSend.ports = dataService.connections.getAllSendPorts();
        udpSend.broadcast = true;
        udpSend.start();
    }

    @Override
    public void run() {
        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(broadcast);
        } catch (SocketException e) {
            UDPErrors.onError(App.instance, UDPErrors.INQUERY_REQUEST, host, destPort, e);
            return;
        }

        if (broadcast)
            sendBroadcast(datagramSocket);
        else
            send(datagramSocket);
    }

    private void send(DatagramSocket datagramSocket) {
        // Get IP
        try {
            if (ip == null) {
                ip = InetAddress.getByName(host);
            }
        } catch (final UnknownHostException e) {
            UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNKNOWN_HOSTNAME, host, destPort, e);
            return;
        }

        for (byte[] message : messages) {
            try {
                datagramSocket.send(new DatagramPacket(message, message.length, ip, destPort));
            } catch (Exception e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNREACHABLE, ip.getHostAddress(), destPort, e);
                else {
                    UDPErrors.onError(App.instance, errorID, ip.getHostAddress(), destPort, e);
                }
                e.printStackTrace();
            }
        }
    }

    private void sendBroadcast(DatagramSocket datagramSocket) {
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
                        ip = address.getBroadcast();
                        if (ip == null) continue;

                        if (logDetect) {
                            String portsString = "";
                            for (Integer port : ports) portsString += String.valueOf(port) + " ";
                            Logging.getInstance().logDetect("UDP Broadcast on " + ip.toString() + " Ports: " + portsString);
                        }

                        Log.w("SendUDPBroadcastJob", "Broadcast Query");

                        //String portString = "";
                        for (int port : ports) {
                            destPort = port;
                            send(datagramSocket);
                            //portString += " " + String.valueOf(port);
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
