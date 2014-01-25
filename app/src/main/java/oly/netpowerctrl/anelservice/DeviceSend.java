package oly.netpowerctrl.anelservice;

import android.content.Context;
import android.util.Log;

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
    private static final int NETWORK_UNKNOWN_HOSTNAME = 3;

    // Singleton
    static DeviceSend mInstance = new DeviceSend();

    public static DeviceSend instance() {
        return mInstance;
    }

    /**
     * Call this to stop the send thread.
     */
    public void interrupt() {
        if (sendThread.isAlive()) {
            sendThread.add(new KillJob());
            try {
                sendThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendThread = new SendThread();
        }

    }

    public void addSendJob(DeviceInfo di, byte[] message, int errorID, boolean retryIfFail) {
        if (!sendThread.isAlive()) {
            try {
                sendThread.start();
            } catch (IllegalThreadStateException ignored) {
                ignored.printStackTrace();
            }
        }
        SendJob job = new SendJob(di, message, errorID);
        if (retryIfFail) {
            new SendJobRepeater(job);
        }
        sendThread.add(job);
    }

    public void addJob(Job job) {
        if (!sendThread.isAlive()) {
            try {
                sendThread.start();
            } catch (IllegalThreadStateException ignored) {
                ignored.printStackTrace();
            }
        }
        sendThread.add(job);
    }

    private static class SendThread extends Thread {
        private LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<Job>();

        public void add(Job job) {
            q.add(job);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Job j = q.take();
                    j.process(DeviceSend.instance());
                } catch (InterruptedException e) {
                    q.clear();
                    return;
                }
            }
        }
    }

    SendThread sendThread = new SendThread();
    DatagramSocket datagramSocket;

    public DeviceSend() {
        try {
            this.datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    static interface Job {
        void process(DeviceSend deviceSend) throws InterruptedException;
    }

    static class KillJob implements Job {
        @Override
        public void process(DeviceSend deviceSend) throws InterruptedException {
            throw new InterruptedException();
        }
    }

    static class WaitJob implements Job {
        @Override
        public void process(DeviceSend deviceSend) {
            // wait 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    static public class SendJob implements Job {
        InetAddress ip = null;
        DeviceInfo di;
        byte[] message;
        int errorID;

        // References to objects
        SendJobRepeater sendJobRepeater = null;

        SendJob(DeviceInfo di, byte[] message, int errorID) {
            this.message = message;
            this.errorID = errorID;
            this.di = di;
        }

        @Override
        public void process(DeviceSend deviceSend) {
            try {
                if (ip == null) {
                    ip = InetAddress.getByName(di.HostName);
                }
                //deviceSend.datagramSocket.setBroadcast(broadcast);
                deviceSend.datagramSocket.send(new DatagramPacket(message, message.length, ip, di.SendPort));
                //Log.w("SendJob",ip.getHostAddress());
                if (sendJobRepeater != null)
                    sendJobRepeater.startDelayedCheck();
            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    DeviceSend.onError(NETWORK_UNREACHABLE, ip.getHostAddress(), di.SendPort, e);
                else {
                    DeviceSend.onError(errorID, ip.getHostAddress(), di.SendPort, e);
                }
            } catch (final UnknownHostException e) {
                DeviceSend.onError(NETWORK_UNKNOWN_HOSTNAME, di.HostName, di.SendPort, e);
            } catch (final Exception e) {
                e.printStackTrace();
                DeviceSend.onError(errorID, di.HostName, di.SendPort, e);
            }

            sendJobRepeater = null;
        }

        public void setRepeater(SendJobRepeater sendJobRepeater) {
            this.sendJobRepeater = sendJobRepeater;
        }
    }

    static public class BroadcastSendJob implements Job {
        private void sendPacket(DeviceSend deviceSend, InetAddress ip, int SendPort, byte[] message) {
            try {
                deviceSend.datagramSocket.setBroadcast(true);
                deviceSend.datagramSocket.send(new DatagramPacket(message, message.length, ip, SendPort));
                //Log.w("BroadcastSendJob",ip.getHostAddress());
            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    DeviceSend.onError(NETWORK_UNREACHABLE, ip.getHostAddress(), SendPort, e);
                else {
                    DeviceSend.onError(INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
                }
            } catch (final Exception e) {
                e.printStackTrace();
                DeviceSend.onError(INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), SendPort, e);
            }
        }

        @Override
        public void process(DeviceSend deviceSend) {
            Set<Integer> ports = NetpowerctrlApplication.instance.getAllSendPorts();
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
                ArrayList<DeviceInfo> devices = NetpowerctrlApplication.instance.configuredDevices;
                for (DeviceInfo di : devices) {
                    deviceSend.addSendJob(di, "wer da?\r\n".getBytes(), INQUERY_REQUEST, false);
                }
            }
        }
    }

    private static void onError(int errorID, String ip, int port, Exception e) {
        Context context = NetpowerctrlApplication.instance;
        if (context == null)
            return;
        String exceptionString = (e == null || e.getMessage() == null) ? "" : e.getMessage();
        switch (errorID) {
            case INQUERY_REQUEST:
                ShowToast.FromOtherThread(context,
                        context.getString(R.string.error_sending_inquiry, ip) + ": " + exceptionString);
                break;
            case INQUERY_BROADCAST_REQUEST:
                ShowToast.FromOtherThread(context,
                        context.getString(R.string.error_sending_broadcast_inquiry, port) + ": " + exceptionString);
                break;
            case NETWORK_UNREACHABLE:
                ShowToast.FromOtherThread(context,
                        context.getString(R.string.error_not_in_range, ip));
                break;
            case NETWORK_UNKNOWN_HOSTNAME:
                ShowToast.FromOtherThread(context,
                        context.getString(R.string.error_not_in_range, ip) + ": " + exceptionString);
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
        addJob(new BroadcastSendJob());
    }

    void sendQuery(DeviceInfo di) {
        addSendJob(di, "wer da?\r\n".getBytes(), INQUERY_REQUEST, false);
    }

    public void sendOutlet(final OutletInfo oi, boolean new_state, boolean retryIfFail) {
        String messageStr = String.format(Locale.US, "%s%d%s%s", new_state ? "Sw_on" : "Sw_off",
                oi.OutletNumber, oi.device.UserName, oi.device.Password);
        addSendJob(oi.device, messageStr.getBytes(), INQUERY_REQUEST, retryIfFail);
    }

    /**
     * Bulk version of sendOutlets. Send changes for multiple outlets of one device in only one packet.
     *
     * @param device_command The device command
     */
    public void sendOutlets(final DeviceCommand device_command, boolean retryIfFail) {
        addSendJob(device_command.device, generateAllOutletsDatagramBytes(device_command),
                INQUERY_REQUEST, retryIfFail);
    }

    /**
     * Bulk version of sendOutlets. Send changes for each device in only one packet per device.
     *
     * @param device_commands Bulk command per device
     */
    public void sendOutlets(final Collection<DeviceCommand> device_commands, boolean retryIfFail) {
        for (DeviceCommand device_command : device_commands) {
            addSendJob(device_command.device, generateAllOutletsDatagramBytes(device_command),
                    INQUERY_REQUEST, retryIfFail);
        }
    }

    public void sendQueries(final Collection<DeviceCommand> device_commands) {
        // request new values from each device
        for (DeviceCommand device_command : device_commands) {
            addSendJob(device_command.device, "wer da?\r\n".getBytes(),
                    INQUERY_REQUEST, false);
        }
    }
}
