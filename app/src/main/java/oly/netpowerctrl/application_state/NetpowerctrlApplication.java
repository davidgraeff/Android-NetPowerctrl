package oly.netpowerctrl.application_state;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.SceneCollection;

/**
 * Application:
 * We keep track of Anel device states via the listener service.
 * Crash management
 */
@ReportsCrashes(formKey = "dGVacG0ydVHnaNHjRjVTUTEtb3FPWGc6MQ",
        mode = ReportingInteractionMode.TOAST,
        mailTo = "david.graeff@web.de",
        forceCloseDialogAfterToast = false, // optional, default false
        additionalSharedPreferences = {SharedPrefs.PREF_WIDGET_BASENAME},
        resToastText = R.string.crash_toast_text)
public class NetpowerctrlApplication extends Application {
    public static NetpowerctrlApplication instance;
    private SceneCollection.IScenesUpdated scenesUpdated = new SceneCollection.IScenesUpdated() {
        @Override
        public void scenesUpdated(boolean addedOrRemoved) {
            StatusNotification.update(NetpowerctrlApplication.instance);
        }
    };
    protected static RuntimeDataController dataController = null;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//                    Log.w("changed", s);
                    if (s.equals(SharedPrefs.PREF_show_persistent_notification))
                        StatusNotification.update(NetpowerctrlApplication.this);
                }
            };

    static public RuntimeDataController getDataController() {
        return dataController;
    }

    public static Handler getMainThreadHandler() {
        return instance.mainThreadHandler;
    }

    /**
     * We do not do any loading or starting when the application is loaded.
     * This can be requested by using useService()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        instance = this;
        dataController = new RuntimeDataController();
        LoadStoreData loadStoreData = new LoadStoreData();
        dataController.setLoadStoreProvider(loadStoreData);
        dataController.loadData(false);
        // show statusBar notification and update each time after changing scenes
        StatusNotification.update(this);
        dataController.sceneCollection.registerObserver(scenesUpdated);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
