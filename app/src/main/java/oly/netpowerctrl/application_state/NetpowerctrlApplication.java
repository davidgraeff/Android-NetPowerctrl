package oly.netpowerctrl.application_state;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceSend;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.network.NetpowerctrlService;
import oly.netpowerctrl.network.ServiceReady;
import oly.netpowerctrl.plugins.PluginController;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Application:
 * * We keep track of Anel device states via the listener service.
 * * Crash management
 */
@ReportsCrashes(formKey = "dGVacG0ydVHnaNHjRjVTUTEtb3FPWGc6MQ",
        mode = ReportingInteractionMode.TOAST,
        mailTo = "david.graeff@web.de",
        forceCloseDialogAfterToast = false, // optional, default false
        additionalSharedPreferences = {SharedPrefs.PREF_BASENAME, SharedPrefs.PREF_GROUPS_BASENAME, SharedPrefs.PREF_WIDGET_BASENAME},
        resToastText = R.string.crash_toast_text)
public class NetpowerctrlApplication extends Application {
    public static NetpowerctrlApplication instance;

    private RuntimeDataController dataController = new RuntimeDataController();
    private int mDiscoverServiceRefCount = 0;
    private NetpowerctrlService mDiscoverService;
    private boolean mWaitForService;
    private PluginController pluginController;
    private ArrayList<ServiceReady> observersServiceReady = new ArrayList<ServiceReady>();

    @SuppressWarnings("unused")
    public boolean registerServiceReadyObserver(ServiceReady o) {
        if (!observersServiceReady.contains(o)) {
            observersServiceReady.add(o);
            if (mDiscoverService != null)
                o.onServiceReady(mDiscoverService);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterServiceReadyObserver(ServiceReady o) {
        observersServiceReady.remove(o);
    }

    private void notifyServiceReady() {
        Iterator<ServiceReady> it = observersServiceReady.iterator();
        while (it.hasNext()) {
            if (!it.next().onServiceReady(mDiscoverService))
                it.remove();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        instance = this;
        dataController.reloadConfiguredDevices();

        // Plugins
        if (SharedPrefs.getLoadExtensions())
            pluginController = new PluginController();


        // Start listen service and listener for wifi changes
        mWaitForService = true;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(instance, NetpowerctrlService.class);
                startService(intent);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        }, 180);
    }

    public void useListener() {
        ++mDiscoverServiceRefCount;
        // Service is not running anymore, restart it
        if (mDiscoverService == null && !mWaitForService) {
            mWaitForService = true;
            Intent intent = new Intent(instance, NetpowerctrlService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void stopUseListener() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0) {
            try {
                mDiscoverService = null;
                mWaitForService = false;
                unbindService(mConnection);
            } catch (IllegalArgumentException ignored) {
            }

            dataController.clear();

            // stop send queue
            DeviceSend.instance().interrupt();

            if (SharedPrefs.notifyOnStop()) {
                ShowToast.FromOtherThread(this, getString(R.string.service_stopped));
            }
        }
    }

    /**
     * Detect new devices and check reachability of configured devices
     */
    private boolean isDetecting = false;

    public void detectNewDevicesAndReachability() {

        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;

        isDetecting = true;
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                isDetecting = false;
            }
        }, 1000);

        // First try a broadcast
        new DeviceQuery(new DeviceUpdateStateOrTimeout() {
            @Override
            public void onDeviceTimeout(DeviceInfo di) {
                di.updated = System.currentTimeMillis();
            }

            @Override
            public void onDeviceUpdated(DeviceInfo di) {
            }

            @Override
            public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
                if (timeout_devices.size() > 0)
                    dataController.notifyConfiguredObservers(timeout_devices);
            }
        });
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NetpowerctrlService.LocalBinder binder = (NetpowerctrlService.LocalBinder) service;
            mDiscoverService = binder.getService();
            mDiscoverService.registerDeviceErrorObserver(dataController);
            mDiscoverService.registerDeviceUpdateObserver(dataController);
            if (mDiscoverServiceRefCount == 0)
                mDiscoverServiceRefCount = 1;
            instance.notifyServiceReady();
            mWaitForService = false;
            // We do a device detection in the wifi change listener already
            detectNewDevicesAndReachability();
        }

        // Service crashed
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDiscoverServiceRefCount = 0;
            mDiscoverService = null;
            mWaitForService = false;
        }
    };

    public NetpowerctrlService getService() {
        return mDiscoverService;
    }

    public PluginController getPluginController() {
        return pluginController;
    }

    static public RuntimeDataController getDataController() {
        return instance.dataController;
    }
}
