package oly.netpowerctrl.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;

public class NetpowerctrlService extends Service {
    private List<DiscoveryThread> discoveryThreads = new ArrayList<DiscoveryThread>();
    private final IBinder mBinder = new LocalBinder();

    private ArrayList<DeviceUpdated> observer = new ArrayList<DeviceUpdated>();
    private ArrayList<DeviceError> errorobserver = new ArrayList<DeviceError>();

    public void registerDeviceUpdateObserver(DeviceUpdated o) {
        observer.add(o);
    }

    public void unregisterDeviceUpdateObserver(DeviceUpdated o) {
        observer.remove(o);
    }

    public void registerDeviceErrorObserver(DeviceError o) {
        errorobserver.add(o);
    }

    public void unregisterDeviceErrorObserver(DeviceError o) {
        errorobserver.remove(o);
    }

    public void notifyObservers(final DeviceInfo di) {
        if (observer == null)
            return;

        Handler h = new Handler(getMainLooper());

        h.post(new Runnable() {
            public void run() {
                for (DeviceUpdated o : observer) {
                    o.onDeviceUpdated(di);
                }
            }
        });
    }

    public void notifyErrorObservers(final String deviceName, final String errMessage) {
        if (observer == null)
            return;

        Handler h = new Handler(getMainLooper());

        h.post(new Runnable() {
            public void run() {
                for (DeviceError o : errorobserver) {
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
        return super.onUnbind(intent);
    }

    private void startDiscoveryThreads() {
        // only start if not yet running
    	if (discoveryThreads.size() == 0) {
	    	for (int port: DeviceQuery.getAllReceivePorts(this)) {
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
