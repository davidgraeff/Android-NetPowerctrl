package oly.netpowerctrl.network;

import android.support.annotation.NonNull;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.main.App;

/**
 * For sending udp packets
 */
public class UDPSend extends Thread {
    @SuppressWarnings("unused")
    private static final String TAG = "SendAndObserveJob";
    final String host;
    final int destPort;
    final List<byte[]> messages = new ArrayList<>();
    final int errorID;
    InetAddress ip = null;
    private boolean broadcast = false;

    public UDPSend(@NonNull InetAddress inetAddress, int destPort, byte[] message, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.errorID = errorID;
        this.host = inetAddress.getHostAddress();
        this.ip = inetAddress;
        this.broadcast = true;
        this.destPort = destPort;
        start();
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

    @Override
    public void run() {
        // Get IP
        try {
            if (ip == null) {
                ip = InetAddress.getByName(host);
            }
        } catch (final UnknownHostException e) {
            UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNKNOWN_HOSTNAME, host, destPort, e);
            return;
        }

        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(broadcast);
        } catch (SocketException e) {
            UDPErrors.onError(App.instance, UDPErrors.INQUERY_REQUEST, host, destPort, e);
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

}
