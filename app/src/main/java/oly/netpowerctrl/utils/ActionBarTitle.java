package oly.netpowerctrl.utils;

import android.app.Activity;

/**
 * Use this helper class to add a "done" and "cancel"
 * actions to the action bar and change the title of the action bar
 * if necessary.
 */
public class ActionBarTitle {
    private CharSequence title_before;

    public void setTitle(Activity activity, int res) {
        title_before = activity.getTitle();
        activity.setTitle(res);
    }

    public void setTitle(Activity activity, String title) {
        title_before = activity.getTitle();
        activity.setTitle(title);
    }

    public void restoreTitle(Activity activity) {
        if (title_before != null)
            activity.setTitle(title_before);
    }
}
