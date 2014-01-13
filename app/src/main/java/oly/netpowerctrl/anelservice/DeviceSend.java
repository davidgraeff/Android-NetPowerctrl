package oly.netpowerctrl.anelservice;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.ShowToast;

/**
 * DeviceSend is a singleton object to send UDP data. It spawns a separate thread
 * for this and enqueues all send jobs.
 */
public class DeviceSend {
    private static final int INQUERY_REQUEST = 0;
    private static final int INQUERY_BROADCAST_REQUEST = 1;
    private static final int NETWORK_UNREACHABLE = 2;

    // Singleton
    static DeviceSend mInstance = new DeviceSend();

    public static DeviceSend instance() {
        return mInstance;
    }

    /**
     * Call this to stop the send thread.
     */
    public void interrupt() {
        if (sendThread.isAlive())
            q.add(new KillJob());
    }

    public void addSendJob(String hostname, int port, byte[] message, boolean broadcast, int errorID) throws UnknownHostException {
        if (!sendThread.isAlive())
            sendThread.start();
        q.add(new SendJob(hostname, port, message, broadcast, errorID));
    }

    public void addSendJob(InetAddress ip, int port, byte[] message, boolean broadcast, int errorID) {
        if (!sendThread.isAlive())
            sendThread.start();
        q.add(new SendJob(ip, port, message, broadcast, errorID));
    }

    private LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<Job>();
    Runnable qProcessor = new Runnable() {
        public void run() {
            while (true) {
                try {
                    Job j = q.take();

                    if (j.stopThread()) {
                        break;
                    } else {
                        j.process();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    };
    Thread sendThread = new Thread(qProcessor);
    DatagramSocket datagramSocket;

    public DeviceSend() {
        try {
            this.datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    static class Job {
        boolean stopThread() {
            return (false);
        }

        void process() {
        }
    }

    static class KillJob extends Job {
        @Override
        boolean stopThread() {
            return (true);
        }
    }

    static class WaitJob extends Job {
        @Override
        void process() {
            // wait 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    class SendJob extends Job {
        InetAddress ip = null;
        String hostname;
        int port;
        byte[] message;
        boolean broadcast;
        int errorID;

        SendJob(InetAddress ip, int port, byte[] message, boolean broadcast, int errorID) {
            this.ip = ip;
            this.message = message;
            this.port = port;
            this.broadcast = broadcast;
            this.errorID = errorID;
        }

        SendJob(String hostname, int port, byte[] message, boolean broadcast, int errorID) {
            this.hostname = hostname;
            this.message = message;
            this.port = port;
            this.broadcast = broadcast;
            this.errorID = errorID;
        }

        @Override
        void process() {
            try {
                if (hostname != null && hostname.length() > 0) {
                    ip = InetAddress.getByName(hostname);
                    hostname = "";
                }
                datagramSocket.setBroadcast(broadcast);
                datagramSocket.send(new DatagramPacket(message, message.length, ip, port));
            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    onError(NETWORK_UNREACHABLE, ip, port, e);
                else {
                    e.printStackTrace();
                    onError(errorID, ip, port, e);
                }
            } catch (final Exception e) {
                e.printStackTrace();
                onError(errorID, ip, port, e);
            }
        }
    }

    private void onError(int errorID, InetAddress ip, int port, Exception e) {
        Context context = NetpowerctrlApplication.instance;
        switch (errorID) {
            case INQUERY_REQUEST:
                ShowToast.FromOtherThread(context,
                        context.getResources().getString(R.string.error_sending_inquiry, ip.getHostAddress()) + ": " + e.getMessage());
                break;
            case INQUERY_BROADCAST_REQUEST:
                ShowToast.FromOtherThread(context,
                        context.getResources().getString(R.string.error_sending_broadcast_inquiry, port) + ": " + e.getMessage());
                break;
            case NETWORK_UNREACHABLE:
                ShowToast.FromOtherThread(context,
                        context.getResources().getString(R.string.error_not_in_range, ip.getHostAddress()));
                break;
        }
    }

    private byte[] generateAllOutletsDatagramBytes(final DeviceCommand device) {
        byte[] data = new byte[3 + device.access.length()];
        data[0] = 'S';
        data[1] = 'w';
        data[2] = device.getSwitchByte();
        System.arraycopy(device.access.getBytes(), 0, data, 3, device.access.length());
        return data;
    }

    void sendBroadcastQuery() {
        Set<Integer> ports = NetpowerctrlApplication.instance.getAllSendPorts();
        boolean foundBroadcastAddresses = false;

        Enumeration list;
        try {
            list = NetworkInterface.getNetworkInterfaces();

            while (list.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) list.nextElement();

                if (iface == null) continue;

                if (!iface.isLoopback() && iface.isUp()) {

                    Iterator it = iface.getInterfaceAddresses().iterator();
                    while (it.hasNext()) {
                        InterfaceAddress address = (InterfaceAddress) it.next();
                        //System.out.println("Found address: " + address);
                        if (address == null) continue;
                        InetAddress broadcast = address.getBroadcast();
                        if (broadcast == null) continue;
                        for (int port : ports)
                            addSendJob(broadcast, port, "wer da?\r\n".getBytes(), true, INQUERY_BROADCAST_REQUEST);
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
            Toast.makeText(NetpowerctrlApplication.instance,
                    NetpowerctrlApplication.instance.getString(R.string.devices_no_new_on_network),
                    Toast.LENGTH_SHORT).show();

            // Query all existing devices directly
            ArrayList<DeviceInfo> devices = NetpowerctrlApplication.instance.configuredDevices;
            for (DeviceInfo di : devices) {
                sendQuery(di.HostName, di.SendPort, false);
            }
        }
    }

    boolean sendQuery(final String hostname, final int port, boolean rangeCheck) {
        if (rangeCheck) {
//            try {
//                if (!isIPinNetworkAddressPool(InetAddress.getByName(hostname))) {
//                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_not_in_range) + ": " + hostname);
//                    return false;
//                }
//            } catch (final SocketException e) {
//                ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_not_in_range) + ": " + hostname+" "+e.getMessage());
//                return false;
//            }
        }
        try {
            addSendJob(hostname, port, "wer da?\r\n".getBytes(), true, INQUERY_REQUEST);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void sendOutlet(final OutletInfo oi, boolean new_state) {
        String messageStr = String.format(Locale.US, "%s%d%s%s", new_state ? "Sw_on" : "Sw_off",
                oi.OutletNumber, oi.device.UserName, oi.device.Password);
        try {
            addSendJob(oi.device.HostName, oi.device.SendPort, messageStr.getBytes(), true, INQUERY_REQUEST);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Bulk version of sendOutlets. Send changes for multiple outlets of a device in only one packet.
     *
     * @param device The device and state
     */
    public void sendOutlets(final DeviceCommand device, final boolean requestNewValuesAfterSend) {
        addSendJob(device.dest, device.port, generateAllOutletsDatagramBytes(device), false, INQUERY_REQUEST);

        if (requestNewValuesAfterSend) {
            q.add(new WaitJob());
            // request new values
            addSendJob(device.dest, device.port, "wer da?\r\n".getBytes(), true, INQUERY_REQUEST);
        }
    }

    /**
     * Bulk version of sendOutlets. Send changes for each device in only one packet per device.
     *
     * @param device_commands Bulk command per device
     */
    public void sendOutlets(final Collection<DeviceCommand> device_commands, final boolean requestNewValuesAfterSend) {
        for (DeviceCommand c : device_commands) {
            addSendJob(c.dest, c.port, generateAllOutletsDatagramBytes(c), false, INQUERY_REQUEST);
        }

        if (requestNewValuesAfterSend) {
            q.add(new WaitJob());
            // request new values from each device
            for (DeviceCommand device_command : device_commands) {
                addSendJob(device_command.dest, device_command.port, "wer da?\r\n".getBytes(), true, INQUERY_REQUEST);
            }
        }
    }
//
//    static private boolean isIPinNetworkAddressPool(InetAddress ip) throws SocketException {
//        byte[] ipAddressBytes = ip.getAddress();
//        // Iterate all NICs (network interface cards)...
//        for (Enumeration networkInterfaceEnumerator = NetworkInterface.getNetworkInterfaces(); networkInterfaceEnumerator.hasMoreElements(); ) {
//            NetworkInterface networkInterface = (NetworkInterface) networkInterfaceEnumerator.nextElement();
//            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
//                InetAddress interfaceIPAddress = interfaceAddress.getAddress();
//                byte[] interfaceIPBytes = interfaceIPAddress.getAddress();
//                if (ipAddressBytes.length != interfaceIPBytes.length)
//                    continue; // different ip versions
//
//                byte[] subNetMaskBytes = ByteBuffer.allocate(4).putInt(interfaceAddress.getNetworkPrefixLength()).array();
//                // check each byte of both addresses while applying the subNet mask
//                for (int i = 0; i < interfaceIPBytes.length; ++i) {
//                    if ((ipAddressBytes[i] & subNetMaskBytes[i]) !=
//                            (interfaceIPBytes[i] & subNetMaskBytes[i]))
//                        continue; // byte not identical, the ip is not for this network interface
//                }
//                return true;
//            }
//        }
//        return false;
//    }
}
