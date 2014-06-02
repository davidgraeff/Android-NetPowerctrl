package oly.netpowerctrl.application_state;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.backup.neighbours.NeighbourDataReceiveService;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;
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
        additionalSharedPreferences = {SharedPrefs.PREF_WIDGET_BASENAME},
        resToastText = R.string.crash_toast_text)
public class NetpowerctrlApplication extends Application {
    public static NetpowerctrlApplication instance;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private RuntimeDataController dataController = null;
    private int mDiscoverServiceRefCount = 0;
    private NetpowerctrlService mDiscoverService;
    private boolean mWaitForService;
    private final ArrayList<ServiceReady> observersServiceReady = new ArrayList<>();

    public boolean isServiceReady() {
        return (mDiscoverService != null);
    }

    @SuppressWarnings("unused")
    public void registerServiceReadyObserver(ServiceReady o) {
        if (!observersServiceReady.contains(o)) {
            observersServiceReady.add(o);
            if (mDiscoverService != null)
                o.onServiceReady();
        }
    }

    @SuppressWarnings("unused")
    public void unregisterServiceReadyObserver(ServiceReady o) {
        observersServiceReady.remove(o);
    }

    private void notifyServiceReady() {
        Iterator<ServiceReady> it = observersServiceReady.iterator();
        while (it.hasNext()) {
            // If onServiceReady return false: remove listener (one-time listener)
            if (!it.next().onServiceReady())
                it.remove();
        }
    }

    private void notifyServiceFinished() {
        for (ServiceReady anObserversServiceReady : observersServiceReady) {
            anObserversServiceReady.onServiceFinished();
        }
    }

    /**
     * We do not do any loading or starting when the application is loaded.
     * This can be requested by using useListener()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        instance = this;
        dataController = new RuntimeDataController();
    }

    private final Handler stopServiceHandler = new Handler();
    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                mDiscoverService = null;
                mWaitForService = false;
                unbindService(mConnection);
            } catch (IllegalArgumentException ignored) {
            }

            dataController.clear();

            if (SharedPrefs.notifyOnStop()) {
                ShowToast.FromOtherThread(NetpowerctrlApplication.this, getString(R.string.service_stopped));
            }
        }
    };

    public void useListener() {
        ++mDiscoverServiceRefCount;
        // Stop delayed stop-service
        stopServiceHandler.removeCallbacks(stopRunnable);
        // Service is not running anymore, restart it
        if (mDiscoverService == null && !mWaitForService) {
            mWaitForService = true;
            Thread t = new Thread() {
                public void run() {
                    dataController.loadData(false);
                    Intent intent = new Intent(instance, NetpowerctrlService.class);
                    startService(intent);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    NeighbourDataReceiveService.startAutoSync();
                }
            };
            t.start();
        }
    }

    public void stopUseListener() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0) {
            stopServiceHandler.postDelayed(stopRunnable, 2000);
        }
    }


    public void findDevices(final DeviceObserverResult callback) {

        // only if the service is available.
        if (mWaitForService)
            return;

        if (mDiscoverService == null) {
            Log.e("findDevices", "No service! Use useListener!");
        } else
            mDiscoverService.findDevices(callback);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NetpowerctrlService.LocalBinder binder = (NetpowerctrlService.LocalBinder) service;
            mDiscoverService = binder.getService();
            if (mDiscoverServiceRefCount == 0)
                mDiscoverServiceRefCount = 1;
            mWaitForService = false;
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Hintergrunddienst gestartet");
            mDiscoverService.start();

            // Notify all observers that we are ready
            notifyServiceReady();
        }

        // Service crashed
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            notifyServiceFinished();
            mDiscoverServiceRefCount = 0;
            mDiscoverService = null;
            mWaitForService = false;
        }
    };

    static public NetpowerctrlService getService() {
        return instance.mDiscoverService;
    }

    static public RuntimeDataController getDataController() {
        return instance.dataController;
    }

    public static Handler getMainThreadHandler() {
        return instance.mainThreadHandler;
    }
}
