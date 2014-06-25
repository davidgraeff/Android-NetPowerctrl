package oly.netpowerctrl.application_state;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.backup.neighbours.NeighbourDataReceiveService;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.Shortcuts;
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

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//                    Log.w("changed", s);
                    if (s.equals(SharedPrefs.PREF_show_persistent_notification))
                        updateNotification();
                }
            };

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

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void updateNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (!SharedPrefs.isNotification()) {
//            Log.w("r","remove");
            mNotificationManager.cancel(1);
            return;
        }

        Intent startMainIntent = new Intent(this, MainActivity.class);
        startMainIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent startMainPendingIntent =
                PendingIntent.getActivity(this, (int) System.currentTimeMillis(), startMainIntent, 0);

        Notification.Builder b = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setSmallIcon(R.drawable.netpowerctrl)
                .setContentIntent(startMainPendingIntent)
                .setOngoing(true);

        //noinspection StatementWithEmptyBody
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            SceneCollection g = NetpowerctrlApplication.getDataController().sceneCollection;
            int maxLength = 0;
            for (Scene scene : g.scenes) {
                if (!scene.isFavourite())
                    continue;
                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = Shortcuts.createShortcutExecutionIntent(this, scene, false, true);
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), clickIntent, 0);

                b.addAction(0, scene.sceneName, pendingIntent);
            }
        } else {
            //            RemoteViews notification_root_layout = new RemoteViews(getPackageName(), R.layout.statusbar_notification);
//            for (int i = 0; i < 4; i++) {
//                RemoteViews textView = new RemoteViews(getPackageName(), R.layout.widget);
//                //textView.setTextViewText(R.id.textView1, "TextView number " + String.valueOf(i));
//                textView.setViewVisibility(R.id.widget_status, View.GONE);
//                textView.setTextViewText(R.id.widget_name, "test");
//                notification_root_layout.addView(R.id.statusbar_notification, textView);
//            }

//            Notification notification = new Notification.Builder(this).
//                    setContent(notification_root_layout).setOngoing(true).
//                    getNotification();
        }

        mNotificationManager.notify(1, b.getNotification());
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
                    // show statusBar notification and update each time after changing scenes
                    updateNotification();
                    dataController.sceneCollection.registerObserver(new SceneCollection.IScenesUpdated() {
                        @Override
                        public void scenesUpdated(boolean addedOrRemoved) {
                            updateNotification();
                        }
                    });

                    // start service
                    Intent intent = new Intent(instance, NetpowerctrlService.class);
                    startService(intent);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                    // neighbour sync
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
