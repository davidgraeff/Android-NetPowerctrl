package oly.netpowerctrl.network;

import android.content.Context;
import android.util.Log;

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
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.utils_gui.ShowToast;

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

    public void start(String threadName) {
        if (sendThread != null && sendThread.isAlive())
            return;
        sendThread = new SendThread(this, threadName);
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
        private final LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<>();
        private final UDPSending UDPSending;

        SendThread(UDPSending UDPSending, String threadName) {
            super(threadName);
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
        final DeviceConnection ci;
        final List<byte[]> messages = new ArrayList<>();
        final int errorID;
        private final DeviceObserverResult deviceObserverResult = new DeviceObserverResult() {
            @Override
            public void onDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                mainLoopHandler.removeCallbacks(redoRunnable);
                mainLoopHandler.removeCallbacks(timeoutRunnable);
            }
        };
        InetAddress ip = null;
        int redoCounter = 0;
        private boolean initialized = false;
        private UDPSending udpSending = null;

        public SendAndObserveJob(DeviceConnection ci, byte[] message, int errorID) {
            this.messages.add(message);
            this.errorID = errorID;
            this.ci = ci;
            assert (ci != null);
        }

        public SendAndObserveJob(DeviceConnection ci, byte[] message, byte[] message2, int errorID) {
            this.messages.add(message);
            this.messages.add(message2);
            this.errorID = errorID;
            this.ci = ci;
            assert (ci != null);
        }

        @Override
        public void process(UDPSending udpSending) {
            this.udpSending = udpSending;
            // Get IP
            try {
                if (ip == null) {
                    ip = InetAddress.getByName(ci.getDestinationHost());
                }
            } catch (final UnknownHostException e) {
                UDPSending.onError(NETWORK_UNKNOWN_HOSTNAME, ci.getDestinationHost(), ci.getDestinationPort(), e);
                return;
            }

            if (!initialized) {
                initialized = true;

                setDeviceQueryResult(deviceObserverResult);
                devices_to_observe = new ArrayList<>();
                devices_to_observe.add(ci.getDevice());

                // Register on main application object to receive device updates
                NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);
            }

            try {
                // Send all messages
                for (byte[] message : messages) {
                    udpSending.datagramSocket.send(new DatagramPacket(message, message.length, ip, ci.getDestinationPort()));
                    Thread.sleep(30);
                }

                if (redoCounter++ < 3)
                    mainLoopHandler.postDelayed(redoRunnable, 400);
                else
                    mainLoopHandler.postDelayed(timeoutRunnable, 400);

            } catch (final SocketException e) {
                if (e.getMessage().contains("ENETUNREACH"))
                    UDPSending.onError(NETWORK_UNREACHABLE, ip.getHostAddress(), ci.getDestinationPort(), e);
                else {
                    UDPSending.onError(errorID, ip.getHostAddress(), ci.getDestinationPort(), e);
                }

            } catch (final Exception e) {
                e.printStackTrace();
                UDPSending.onError(errorID, ci.getDestinationHost(), ci.getDestinationPort(), e);
            }
        }

        @Override
        protected void doAction(Device device, boolean repeated) {
            if (udpSending != null)
                udpSending.addJob(this);
        }
    }


    static public class SendRawJob implements Job {
        final byte[] message;
        final int sendPort;
        public InetAddress ip = null;

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
