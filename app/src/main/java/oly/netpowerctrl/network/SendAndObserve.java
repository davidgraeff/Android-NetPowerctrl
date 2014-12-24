package oly.netpowerctrl.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Created by david on 17.12.14.
 */
public class SendAndObserve extends DeviceObserverBase {
    @SuppressWarnings("unused")
    private static final String TAG = "SendAndObserveJob";
    final DeviceConnection ci;
    final List<byte[]> messages = new ArrayList<>();
    final int errorID;
    final private WeakReference<UDPSending> udpSendingReference;
    InetAddress ip = null;
    private AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(Void... voids) {
            // Get IP
            try {
                if (ip == null) {
                    ip = InetAddress.getByName(ci.getDestinationHost());
                }
            } catch (final UnknownHostException e) {
                UDPSending.onError(App.instance, UDPSending.NETWORK_UNKNOWN_HOSTNAME, ci.getDestinationHost(), ci.getDestinationPort(), e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (!aBoolean) return;

            final PluginService service = PluginService.getService();
            if (service == null)
                return;

            UDPSending udpSending = udpSendingReference.get();
            if (udpSending == null)
                return;

            for (byte[] message : messages) {
                jobs.add(new UDPSending.SendRawJob(udpSending, message, ip, ci.getDestinationPort()));
            }

            clearDevicesToObserve();
            addDevice(service.getAppData(), ci.getDevice());
            startQuery();
        }
    };
    private List<UDPSending.SendRawJob> jobs = new ArrayList<>();

    public SendAndObserve(@NonNull UDPSending udpSending, @NonNull Context context,
                          @NonNull DeviceConnection ci, byte[] message, int errorID) {
        super(context, null, 200, 3);
        this.udpSendingReference = new WeakReference<>(udpSending);
//            Log.w(TAG, "SendAndObserveJob");
        this.messages.add(message);
        this.errorID = errorID;
        this.ci = ci;
        asyncTask.execute();
    }

    public SendAndObserve(@NonNull UDPSending udpSending, @NonNull Context context,
                          @NonNull DeviceConnection ci, byte[] message, byte[] message2, int errorID) {
        super(context, null, 200, 3);
        this.udpSendingReference = new WeakReference<>(udpSending);
//            Log.w(TAG, "SendAndObserveJob");
        this.messages.add(message);
        this.messages.add(message2);
        this.errorID = errorID;
        this.ci = ci;
        asyncTask.execute();
    }

    @Override
    protected void doAction(@Nullable Device device, int remainingRepeats) {
        UDPSending udpSending = udpSendingReference.get();
        if (udpSending == null) return;
        for (UDPSending.SendRawJob job : jobs) {
            udpSending.addJob(job);
        }
    }
}
