package oly.netpowerctrl.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.utils.ShowToast;

/**
 * udpSending spawns a separate thread for UDP sending and enqueues all send jobs.
 * It is used in the main service and for the neighbour discovery.
 */
public class UDPSending {
    public static final int INQUERY_REQUEST = 0;
    public static final int INQUERY_BROADCAST_REQUEST = 1;
    public static final int NETWORK_UNREACHABLE = 2;
    private static final int NETWORK_UNKNOWN_HOSTNAME = 3;

    public DatagramSocket datagramSocket;
    private SendThread sendThread = null;

    public UDPSending(boolean forBroadcast) {
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(forBroadcast);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static void onError(int errorID, String ip, int port, Exception e) {
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

    /**
     * Call this to stop the send thread.
     */
    public void interrupt() {
        if (sendThread != null && sendThread.isAlive()) {
            sendThread.add(new KillJob());
            try {
                sendThread.join();
            } catch (InterruptedException ignored) {
            }
            sendThread = null;
        }
    }

    public boolean isRunning() {
        return sendThread != null && sendThread.isAlive();
    }

    public void start() {
        if (sendThread != null && sendThread.isAlive())
            return;
        sendThread = new SendThread(this);
    }

    public void addJob(Job job) {
        if (sendThread == null)
            return;
        if (!sendThread.isAlive()) {
            try {
                sendThread.start();
            } catch (IllegalThreadStateException ignored) {
                ignored.printStackTrace();
            }
        }
        sendThread.add(job);
    }

    public static interface Job {
        void process(UDPSending UDPSending) throws InterruptedException;
    }

    private static class SendThread extends Thread {
        private final LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<Job>();
        private final UDPSending UDPSending;

        SendThread(UDPSending UDPSending) {
            this.UDPSending = UDPSending;
        }

        public void add(Job job) {
            q.add(job);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Job j = q.take();
                    j.process(UDPSending);
                } catch (InterruptedException e) {
                    q.clear();
                    return;
                }
            }
        }
    }

    static class KillJob implements Job {
        @Override
        public void process(UDPSending UDPSending) throws InterruptedException {
            throw new InterruptedException();
        }
    }

    static class WaitJob implements Job {
        @Override
        public void process(UDPSending UDPSending) {
            // wait 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    static public class SendAndObserveJob extends DeviceObserverBase implements Job {
        InetAddress ip = null;
        final DeviceInfo di;
        final List<byte[]> messages = new ArrayList<byte[]>();
        final int errorID;
        int redoCounter = 0;
        private boolean initialized = false;
        private final long current_time = System.currentTimeMillis();
        private UDPSending udpSending = null;

        private final DeviceObserverResult deviceObserverResult = new DeviceObserverResult() {

            @Override
            public void onDeviceError(DeviceInfo di) {
            }

            @Override
            public void onDeviceTimeout(DeviceInfo di) {
            }

            @Override
            public void onDeviceUpdated(DeviceInfo di) {
            }

            @Override
            public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
                mainLoopHandler.removeCallbacks(redoRunnable);
                mainLoopHandler.removeCallbacks(timeoutRunnable);
                if (timeout_devices.isEmpty())
                    return;

                Context context = NetpowerctrlApplication.instance;
                Toast.makeText(context,
                        context.getString(R.string.error_setting_outlet, di.DeviceName,
                                (int) ((current_time - di.getUpdatedTime()) / 1000)),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        public SendAndObserveJob(DeviceInfo di, byte[] message, int errorID) {
            this.messages.add(message);
            this.errorID = errorID;
            this.di = di;
        }

        public SendAndObserveJob(DeviceInfo di, byte[] message, byte[] message2, int errorID) {
            this.messages.add(message);
            this.messages.add(message2);
            this.errorID = errorID;
            this.di = di;
        }

        @Override
        public void process(UDPSending udpSending) {
            this.udpSending = udpSending;
            // Get IP
            try {
                if (ip == null) {
                    ip = InetAddress.getByName(di.HostName);
                }
            } catch (final UnknownHostException e) {
                UDPSending.onError(NETWORK_UNKNOWN_HOSTNAME, di.HostName, di.SendPort, e);
                return;
            }

            if (!initialized) {
                initialized = true;

                //DeviceObserverBase
                setDeviceQueryResult(deviceObserverResult);
                devices_to_observe = new ArrayList<DeviceInfo>();
                devices_to_observe.add(di);

                // Register on main application object to receive device updates
                NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);
            }

            try {
                // Send all messages
                for (byte[] message : messages) {
                    udpSending.datagramSocket.send(new DatagramPacket(message, message.length, ip, di.SendPort));
                    Thread.sleep(30);
                }

                if (redoCounter++ < 3)
                    mainLoopHandler.postDelayed(redoRunnable, 400);
                else
                    mainLoopHandler.postDelayed(timeoutRunnable, 400);

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPSending.onError(NETWORK_UNREACHABLE, ip.getHostAddress(), di.SendPort, e);
                else {
                    UDPSending.onError(errorID, ip.getHostAddress(), di.SendPort, e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPSending.onError(errorID, di.HostName, di.SendPort, e);
            }
        }

        @Override
        protected void doAction(DeviceInfo di, boolean repeated) {
            if (udpSending != null)
                udpSending.addJob(this);
        }
    }


    static public class SendRawJob implements Job {
        public InetAddress ip = null;
        final byte[] message;
        int sendPort;

        public SendRawJob(byte[] message, InetAddress ip, int sendPort) {
            this.message = message;
            this.sendPort = sendPort;
            this.ip = ip;
        }

        /**
         * Broadcast job
         *
         * @param message
         * @param sendPort
         */
        public SendRawJob(byte[] message, int sendPort) {
            this.message = message;
            this.sendPort = sendPort;
            this.ip = Utils.getBroadcast(Utils.getIpv4Address());
            if (ip == null) {
                Log.e("sendJob", "broadcast address failed");
            }
        }

        @Override
        public void process(UDPSending udpSending) {
            if (ip == null)
                return;

            try {
                udpSending.datagramSocket.send(new DatagramPacket(message, message.length, ip, sendPort));

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPSending.onError(NETWORK_UNREACHABLE, ip.getHostAddress(), sendPort, e);
                else {
                    UDPSending.onError(INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), sendPort, e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPSending.onError(INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), sendPort, e);
            }
        }
    }
}
