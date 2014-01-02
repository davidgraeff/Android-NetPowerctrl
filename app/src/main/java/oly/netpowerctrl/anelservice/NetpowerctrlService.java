package oly.netpowerctrl.anelservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.preferences.SharedPrefs;

public class NetpowerctrlService extends Service {
    private List<DiscoveryThread> discoveryThreads = new ArrayList<DiscoveryThread>();
    private final IBinder mBinder = new LocalBinder();

    private ArrayList<DeviceUpdate> observer = new ArrayList<DeviceUpdate>();
    private ArrayList<DeviceError> errorObserver = new ArrayList<DeviceError>();

    public void registerDeviceUpdateObserver(DeviceUpdate o) {
        observer.add(o);
    }

    public void unregisterDeviceUpdateObserver(DeviceUpdate o) {
        observer.remove(o);
    }

    public void registerDeviceErrorObserver(DeviceError o) {
        errorObserver.add(o);
    }

    public void unregisterDeviceErrorObserver(DeviceError o) {
        errorObserver.remove(o);
    }

    public void notifyObservers(final DeviceInfo di) {
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

    public class LocalBinder extends Binder {
        public NetpowerctrlService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetpowerctrlService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        startDiscoveryThreads();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopDiscoveryThreads();
        stopSelf();
        return super.onUnbind(intent);
    }

    private void startDiscoveryThreads() {
        // only start if not yet running
        if (discoveryThreads.size() == 0) {
            for (int port : SharedPrefs.getAllReceivePorts(this)) {
                DiscoveryThread thr = new DiscoveryThread(port, this);
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
        for (DiscoveryThread thr : discoveryThreads)
            thr.interrupt();
        discoveryThreads.clear();
        // socket needs minimal time to really go away
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }
}
