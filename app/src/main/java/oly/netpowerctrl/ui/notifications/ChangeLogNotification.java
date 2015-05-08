package oly.netpowerctrl.ui.notifications;

import android.app.Activity;

import com.rey.material.widget.SnackBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.ChangeLogUtil;

/**
 * Change log notification
 */
public class ChangeLogNotification extends PermanentNotification {
    public ChangeLogNotification(Activity activity) {
        super("changelog", activity);
    }

    @Override
    public String getText() {
        return App.getAppString(R.string.notification_title_changelog, SharedPrefs.getVersionName(App.instance));
    }

    @Override
    public boolean hasCloseButton() {
        return true;
    }

    @Override
    public void onDismiss() {
        SharedPrefs.getInstance().acceptUpdatedVersion();
    }

    @Override
    public void action(SnackBar snackBar) {
        Activity activity = activityWeakReference.get();
        if (activity != null) {
            ChangeLogUtil.showChangeLog(activity);
        }
        snackBar.dismiss();
    }

    @Override
    public String getActionButtonText() {
        return App.getAppString(R.string.changelog_title);
    }
}
