package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.utils.statusbar_and_speech.AndroidStatusBarService;
import oly.netpowerctrl.widget.WidgetUpdateService;

/**
 * For starting the android status bar service and the listen service.
 */
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private int resumed;
    private int started;
    private boolean firstStart = true;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (firstStart) {
            firstStart = false;
            LoadStoreIconData.onCreate(activity);
            WidgetUpdateService.ForceUpdateAll(activity);
            AndroidStatusBarService.startOrStop(App.instance);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (started == 0) {
            PluginService.useService(new WeakReference<Object>(activity));
            AppData.useAppData();
        }
        ++started;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        --started;
        //PowerManager pm =(PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (started <= 0) { // pm.isScreenOn() == false
            if (SharedPrefs.getInstance().isMaximumEnergySaving()) {
                Log.w("EnergySave", "onHide");
                PluginService.stopUseService(activity);
            }
        }
    }

    public void onBackground() {
        if (!SharedPrefs.getInstance().isMaximumEnergySaving()) {
            Log.w("EnergySave", "onBackground");
            PluginService.stopUseService(this);
        }
    }
}