package oly.netpowerctrl.network;

import android.content.Context;
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
 * DeviceSend is a singleton object to send UDP data. It spawns a separate thread
 * for this and enqueues all send jobs.
 */
public class DeviceSend {
    public static final int INQUERY_REQUEST = 0;
    public static final int INQUERY_BROADCAST_REQUEST = 1;
    public static final int NETWORK_UNREACHABLE = 2;
    private static final int NETWORK_UNKNOWN_HOSTNAME = 3;

    // Singleton
    static DeviceSend mInstance = new DeviceSend();
    public DatagramSocket datagramSocket;
    SendThread sendThread = null;

    public DeviceSend() {
        try {
            this.datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static DeviceSend instance() {
        return mInstance;
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
            } catch (InterruptedException e) {
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
        sendThread = new SendThread();
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
        void process(DeviceSend deviceSend) throws InterruptedException;
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

    static public class SendJob extends DeviceObserverBase implements Job {
        InetAddress ip = null;
        DeviceInfo di;
        byte[] message;
        int errorID;
        private boolean initialized = false;
        private long current_time = System.currentTimeMillis();

        private DeviceQueryResult deviceQueryResult = new DeviceQueryResult() {

            @Override
            public void onDeviceError(DeviceInfo di, String error_message) {

            }

            @Override
            public void onDeviceTimeout(DeviceInfo di) {

            }

            @Override
            public void onDeviceUpdated(DeviceInfo di) {

            }

            @Override
            public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
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

        public SendJob(DeviceInfo di, byte[] message, int errorID) {
            this.message = message;
            this.errorID = errorID;
            this.di = di;
        }

        @Override
        public void process(DeviceSend deviceSend) {
            if (!initialized) {
                initialized = true;

                //DeviceObserverBase
                setDeviceQueryResult(deviceQueryResult);
                devices_to_observe = new ArrayList<DeviceInfo>();
                devices_to_observe.add(di);

                // Register on main application object to receive device updates
                NetpowerctrlApplication.getDataController().addUpdateDeviceState(this);

                mainLoopHandler.postDelayed(timeoutRunnable, 700);
            }

            try {
                if (ip == null) {
                    ip = InetAddress.getByName(di.HostName);
                }
                //deviceSend.datagramSocket.setBroadcast(broadcast);
                deviceSend.datagramSocket.send(new DatagramPacket(message, message.length, ip, di.SendPort));
                mainLoopHandler.postDelayed(redoRunnable, 300);

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
        }

        @Override
        protected void doAction(DeviceInfo di) {
            DeviceSend.instance().addJob(this);
        }
    }
}
