package oly.netpowerctrl.network;

import android.content.Context;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

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

    public static void onError(Context context, int errorID, String ip, int port, Exception e) {
        String exceptionString = (e == null || e.getMessage() == null) ? "" : e.getMessage();
        switch (errorID) {
            case INQUERY_REQUEST:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_sending_inquiry, ip) + ": " + exceptionString);
                break;
            case INQUERY_BROADCAST_REQUEST:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_sending_broadcast_inquiry, port) + ": " + exceptionString);
                break;
            case NETWORK_UNREACHABLE:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_not_in_range, ip));
                break;
            case NETWORK_UNKNOWN_HOSTNAME:
                InAppNotifications.FromOtherThread(context,
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
        }
        sendThread = null;
    }

    public boolean isRunning() {
        return sendThread != null && sendThread.isAlive();
    }

    public void start(String threadName) {
        if (sendThread != null && sendThread.isAlive())
            return;
        sendThread = new SendThread(threadName);
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
        void process() throws InterruptedException;
    }

    private static class SendThread extends Thread {
        private final LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<>();

        SendThread(String threadName) {
            super(threadName);
        }

        public void add(Job job) {
            q.add(job);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Job j = q.take();
                    j.process();
                } catch (InterruptedException e) {
                    q.clear();
                    return;
                }
            }
        }
    }

    static class KillJob implements Job {
        @Override
        public void process() throws InterruptedException {
            throw new InterruptedException();
        }
    }

    static class WaitJob implements Job {
        @Override
        public void process() {
            // wait 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    static public class SendAndObserveJob extends DeviceObserverBase implements Job {
        @SuppressWarnings("unused")
        private static final String TAG = "SendAndObserveJob";
        final DeviceConnection ci;
        final List<byte[]> messages = new ArrayList<>();
        final int errorID;
        private final onDeviceObserverResult deviceObserverResult = new onDeviceObserverResult() {
            @Override
            public void onObserverDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                mainLoopHandler.removeCallbacks(redoRunnable);
                mainLoopHandler.removeCallbacks(timeoutRunnable);
            }
        };
        final private WeakReference<UDPSending> udpSendingReference;
        InetAddress ip = null;
        int redoCounter = 0;
        private boolean initialized = false;

        public SendAndObserveJob(UDPSending udpSending, Context context, DeviceConnection ci, byte[] message, int errorID) {
            super(context, null);
            this.udpSendingReference = new WeakReference<>(udpSending);
//            Log.w(TAG, "SendAndObserveJob");
            this.messages.add(message);
            this.errorID = errorID;
            this.ci = ci;
            assert (ci != null);
        }

        public SendAndObserveJob(UDPSending udpSending, Context context, DeviceConnection ci, byte[] message, byte[] message2, int errorID) {
            super(context, null);
            this.udpSendingReference = new WeakReference<>(udpSending);
//            Log.w(TAG, "SendAndObserveJob");
            this.messages.add(message);
            this.messages.add(message2);
            this.errorID = errorID;
            this.ci = ci;
            assert (ci != null);
        }

        @Override
        public void process() {
            Context context = ListenService.getService();
            if (context == null)
                return;

            // Get IP
            try {
                if (ip == null) {
                    ip = InetAddress.getByName(ci.getDestinationHost());
                }
            } catch (final UnknownHostException e) {
                UDPSending.onError(context, NETWORK_UNKNOWN_HOSTNAME, ci.getDestinationHost(), ci.getDestinationPort(), e);
                return;
            }

            if (!initialized) {
                initialized = true;

                setDeviceQueryResult(deviceObserverResult);
                clearDevicesToObserve();
                addDevice(ci.getDevice(), false);

                // Register on main application object to receive device updates
                App.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        AppData.getInstance().addUpdateDeviceState(SendAndObserveJob.this);
                    }
                });
            }

            UDPSending udpSending = udpSendingReference.get();
            if (udpSending == null)
                return;
            DatagramSocket datagramSocket = udpSending.datagramSocket;

            try {
                // Send all messages
                for (byte[] message : messages) {
                    datagramSocket.send(new DatagramPacket(message, message.length, ip, ci.getDestinationPort()));
                    Thread.sleep(30);
                }

                if (redoCounter++ < 3)
                    mainLoopHandler.postDelayed(redoRunnable, 400);
                else
                    mainLoopHandler.postDelayed(timeoutRunnable, 400);

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPSending.onError(context, NETWORK_UNREACHABLE, ip.getHostAddress(), ci.getDestinationPort(), e);
                else {
                    UDPSending.onError(context, errorID, ip.getHostAddress(), ci.getDestinationPort(), e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPSending.onError(context, errorID, ci.getDestinationHost(), ci.getDestinationPort(), e);
            }
        }

        @Override
        protected void doAction(Device device, boolean repeated) {
            UDPSending udpSending = udpSendingReference.get();
            if (udpSending != null)
                udpSending.addJob(this);
        }
    }


    static public class SendRawJob implements Job {
        final byte[] message;
        final int sendPort;
        final private WeakReference<UDPSending> udpSendingReference;
        public InetAddress ip = null;

        public SendRawJob(UDPSending udpSending, byte[] message, InetAddress ip, int sendPort) {
            this.udpSendingReference = new WeakReference<>(udpSending);
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
        public SendRawJob(UDPSending udpSending, byte[] message, int sendPort) {
            this.udpSendingReference = new WeakReference<>(udpSending);
            this.message = message;
            this.sendPort = sendPort;
            this.ip = Utils.getBroadcast(Utils.getIpv4Address());
            if (ip == null) {
                Log.e("sendJob", "broadcast address failed");
            }
        }

        @Override
        public void process() {
            if (ip == null)
                return;

            Context context = ListenService.getService();
            if (context == null)
                return;

            UDPSending udpSending = udpSendingReference.get();
            if (udpSending == null)
                return;
            DatagramSocket datagramSocket = udpSending.datagramSocket;

            try {
                datagramSocket.send(new DatagramPacket(message, message.length, ip, sendPort));

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPSending.onError(context, NETWORK_UNREACHABLE, ip.getHostAddress(), sendPort, e);
                else {
                    UDPSending.onError(context, INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), sendPort, e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPSending.onError(context, INQUERY_BROADCAST_REQUEST, ip.getHostAddress(), sendPort, e);
            }
        }
    }
}
