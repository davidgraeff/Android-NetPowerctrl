package oly.netpowerctrl.network;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelDeviceDiscoveryThread;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

public class NetpowerctrlService extends Service {
    private List<AnelDeviceDiscoveryThread> discoveryThreads = new ArrayList<AnelDeviceDiscoveryThread>();
    private final IBinder mBinder = new LocalBinder();

    private ArrayList<DeviceUpdate> observer = new ArrayList<DeviceUpdate>();
    private ArrayList<DeviceError> errorObserver = new ArrayList<DeviceError>();

    private DeviceInfo temporary_device = null;

    public void registerDeviceUpdateObserver(DeviceUpdate o) {
        if (!observer.contains(o))
            observer.add(o);
    }

    public void unregisterDeviceUpdateObserver(DeviceUpdate o) {
        observer.remove(o);
    }

    public void registerDeviceErrorObserver(DeviceError o) {
        if (!errorObserver.contains(o))
            errorObserver.add(o);
    }

    public void unregisterDeviceErrorObserver(DeviceError o) {
        errorObserver.remove(o);
    }

    public void notifyObservers(final DeviceInfo di) {
        //Log.w("LISTEN_SERVICE", "UPDATE "+di.HostName);
        assert di != null;
        if (observer.isEmpty())
            return;

        Handler h = new Handler(getMainLooper());

        h.post(new Runnable() {
            public void run() {
                for (DeviceUpdate o : observer) {
                    o.onDeviceUpdated(di);
                }
            }
        });
    }

    public void notifyErrorObservers(final String deviceName, final String errMessage) {
        if (errorObserver.isEmpty())
            return;

        Handler h = new Handler(getMainLooper());

        h.post(new Runnable() {
            public void run() {
                for (DeviceError o : errorObserver) {
                    o.onDeviceError(deviceName, errMessage);
                }
            }
        });
    }

    public void removeTemporaryDevice(DeviceInfo device) {
        if (temporary_device == device)
            temporary_device = null;
    }

    public void replaceTemporaryDevice(DeviceInfo device) {
        temporary_device = device;
    }

    public class LocalBinder extends Binder {
        public NetpowerctrlService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetpowerctrlService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Listen for wifi/network changes
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        lastNetworkType = (cm.getActiveNetworkInfo() == null) ? 0 : cm.getActiveNetworkInfo().getType();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(networkChangedListener, filter);

        // Start listen and send threads
        checkIfNetworkReachable();
        return mBinder;
    }

    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    public boolean isNetworkReducedMode;

    /**
     * Will be set if no one of the network DeviceInfo's is reachable at the moment.
     */
    public void setNetworkReducedMode() {
        isNetworkReducedMode = true;
        boolean alreadyRunning = DeviceSend.instance().isRunning();
        if (!alreadyRunning)
            return; // Nothing to do.

        // Stop send and listen threads
        DeviceSend.instance().interrupt();
        stopDiscoveryThreads();

        if (SharedPrefs.notifyOnStop()) {
            ShowToast.FromOtherThread(this, getString(R.string.energy_saving_mode));
        }
    }

    private void checkIfNetworkReachable() {
        isNetworkReducedMode = false;
        boolean alreadyRunning = DeviceSend.instance().isRunning();
        if (!alreadyRunning) { // Start send and listen threads
            DeviceSend.instance().start();
            startDiscoveryThreads();
        }
        NetpowerctrlApplication.instance.detectNewDevicesAndReachability();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(networkChangedListener);
        stopDiscoveryThreads();
        stopSelf();
        return super.onUnbind(intent);
    }

    private int lastNetworkType = 0;
    BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            int type = (cm.getActiveNetworkInfo() == null) ? 0 : cm.getActiveNetworkInfo().getType();
            // Reset all lastUpdated counter. We will output no error message to the user if a device
            // is not reachable and the lastUpdated counter is zero.
            List<DeviceInfo> devices = NetpowerctrlApplication.getDataController().configuredDevices;
            for (DeviceInfo di : devices) {
                di.updated = 0;
            }

            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())
                checkIfNetworkReachable();
            else
                setNetworkReducedMode();
//            if (type == lastNetworkType)
//                return;
//            lastNetworkType = type;

            //Log.w("NETWORK",Integer.valueOf(lastNetworkType).toString());

        }
    };

    private void startDiscoveryThreads() {
        // only start if not yet running
        if (discoveryThreads.size() == 0) {
            Set<Integer> ports = NetpowerctrlApplication.getDataController().getAllReceivePorts();
            if (temporary_device != null)
                ports.add(temporary_device.ReceivePort);

            for (int port : ports) {
                AnelDeviceDiscoveryThread thr = new AnelDeviceDiscoveryThread(port, this);
                thr.start();
                discoveryThreads.add(thr);
            }
            // give the threads a chance to start
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void restartDiscoveryThreads() {
        stopDiscoveryThreads();
        startDiscoveryThreads();
    }

    private void stopDiscoveryThreads() {
        if (discoveryThreads.size() == 0)
            return;

        for (AnelDeviceDiscoveryThread thr : discoveryThreads)
            thr.interrupt();
        discoveryThreads.clear();
        // socket needs minimal time to really go away
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }
}
