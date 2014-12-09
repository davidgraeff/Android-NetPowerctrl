package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.utils.AndroidStatusBarService;
import oly.netpowerctrl.widget.WidgetUpdateService;

/**
 * For starting the android status bar service and the listen service.
 */
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private int resumed;
    private int started;
    private int created;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (created == 0) {
            LoadStoreIconData.onCreate(activity);
            WidgetUpdateService.ForceUpdateAll(activity);
            AndroidStatusBarService.startOrStop(App.instance);
        }
        ++created;
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        --created;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        --resumed;
        android.util.Log.w("test", "application is in foreground: " + (resumed > 0));
        if (resumed <= 0) {
            if (SharedPrefs.getInstance().isMaximumEnergySaving())
                PluginService.stopUseService();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (started == 0) {
            PluginService.useService();
            AppData.useAppData();
        }
        ++started;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        --started;
        android.util.Log.w("test", "application is visible: " + (started > 0));
        if (started <= 0) {
            if (!SharedPrefs.getInstance().isMaximumEnergySaving())
                PluginService.stopUseService();
        }
    }
}