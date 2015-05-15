package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.App;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.status_bar.AndroidStatusBarService;
import oly.netpowerctrl.widget.ProviderExecutable;
import oly.netpowerctrl.widget.ProviderGroup;
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
            WidgetUpdateService.ForceUpdateAll(activity, ProviderExecutable.class);
            WidgetUpdateService.ForceUpdateAll(activity, ProviderGroup.class);
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
            DataService.useService(new WeakReference<Object>(this));
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
                DataService.stopUseService(this);
            }
        }
    }

    public void onBackground() {
        if (SharedPrefs.getInstance().isMaximumEnergySaving()) {
            Log.w("EnergySave", "onBackground");
            DataService.stopUseService(this);
        }
    }
}