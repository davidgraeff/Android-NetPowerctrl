package oly.netpowerctrl.network;

import android.support.annotation.NonNull;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.main.App;

/**
 * Created by david on 17.12.14.
 */
public class UDPSend extends Thread {
    @SuppressWarnings("unused")
    private static final String TAG = "SendAndObserveJob";
    final IOConnection ci;
    final List<byte[]> messages = new ArrayList<>();
    final int errorID;
    private final DataService dataService;
    InetAddress ip = null;

    public UDPSend(@NonNull DataService service,
                   @NonNull IOConnection ci, byte[] message, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.dataService = service;
        this.errorID = errorID;
        this.ci = ci;
        start();
    }

    public UDPSend(@NonNull DataService service,
                   @NonNull IOConnection ci, byte[] message, byte[] message2, int errorID) {
        super("UDPSend");
        this.messages.add(message);
        this.messages.add(message2);
        this.dataService = service;
        this.errorID = errorID;
        this.ci = ci;
        start();
    }

    @Override
    public void run() {
        // Get IP
        try {
            if (ip == null) {
                ip = InetAddress.getByName(ci.getDestinationHost());
            }
        } catch (final UnknownHostException e) {
            UDPErrors.onError(App.instance, UDPErrors.NETWORK_UNKNOWN_HOSTNAME, ci.getDestinationHost(), ci.getDestinationPort(), e);
            return;
        }

        DatagramSocket datagramSocket;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(false);
        } catch (SocketException e) {
            UDPErrors.onError(App.instance, UDPErrors.INQUERY_REQUEST, ci.getDestinationHost(), ci.getDestinationPort(), e);
            return;
        }

        for (byte[] message : messages) {
            if (!UDPErrors.sendPacketHandleErrors(dataService, datagramSocket, ip, ci.getDestinationPort(), message))
                return;
        }
    }

}
