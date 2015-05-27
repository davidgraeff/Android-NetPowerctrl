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
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.App;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.utils.Logging;

/**
 * For sending udp packets
 */
public class UDPSend extends Thread {
    private static UDPSend udpSend = null;
    private LinkedBlockingQueue<UDPCommand> q = new LinkedBlockingQueue<>();

    private UDPSend() {
        super("UDPSend");
        start();
    }

    public static void killSendThread() {
        if (udpSend == null || udpSend.q == null) return;
        Log.w("UDPSEND", "killSendThread");
        UDPCommand command = new UDPCommand();
        udpSend.q.add(command);
    }

    public static void sendBroadcast(Set<Integer> ports, byte[] message) {
        if (ports.isEmpty()) return;
        UDPCommand command = new UDPCommand();
        command.messages.add(message);
        command.errorID = UDPErrors.INQUERY_BROADCAST_REQUEST;
        command.ports = ports;
        if (udpSend == null) udpSend = new UDPSend();
        udpSend.q.add(command);
    }

    public static void sendMessage(@NonNull IOConnectionUDP ioConnection, byte[] message) {
        UDPCommand command = new UDPCommand();
        command.messages.add(message);
        command.errorID = UDPErrors.INQUERY_REQUEST;
        command.host = ioConnection.getDestinationHost();
        command.destPort = ioConnection.getDestinationPort();
        if (udpSend == null) udpSend = new UDPSend();
        udpSend.q.add(command);
    }

    private static void send(DatagramSocket datagramSocket, UDPCommand j) {
        // Get IP
        try {
            if (j.ip == null) {
                j.ip = InetAddress.getByName(j.host);
            }
        } catch (final UnknownHostException e) {
            UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNKNOWN_HOSTNAME, j.host, j.destPort, e);
            return;
        }

        for (byte[] message : j.messages) {
            try {
                datagramSocket.send(new DatagramPacket(message, message.length, j.ip, j.destPort));
            } catch (Exception e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNREACHABLE, j.ip.getHostAddress(), j.destPort, e);
                else {
                    UDPErrors.onError(App.instance, j.errorID, j.ip.getHostAddress(), j.destPort, e);
                }
                e.printStackTrace();
            }
        }
    }

    private static void sendBroadcast(DatagramSocket datagramSocket, UDPCommand j) {
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
                        j.ip = address.getBroadcast();
                        if (j.ip == null) continue;

                        if (logDetect) {
                            String portsString = "";
                            for (Integer port : j.ports) portsString += String.valueOf(port) + " ";
                            Logging.getInstance().logDetect("UDP Broadcast on " + j.ip.toString() + " Ports: " + portsString);
                        }

                        Log.w("SendUDPBroadcastJob", "Broadcast Query");

                        //String portString = "";
                        for (int port : j.ports) {
                            j.destPort = port;
                            send(datagramSocket, j);
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

    @Override
    public void run() {
        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            UDPCommand j;
            try {
                j = q.take();
                if (j.messages.isEmpty()) return;
            } catch (InterruptedException e) {
                break;
            }

            if (j.ports != null)
                sendBroadcast(datagramSocket, j);
            else
                send(datagramSocket, j);
        }

        udpSend = null;
    }

    private static class UDPCommand {
        String host;
        int destPort;
        Set<Integer> ports;
        List<byte[]> messages = new ArrayList<>();
        int errorID;
        InetAddress ip = null;
    }

}
