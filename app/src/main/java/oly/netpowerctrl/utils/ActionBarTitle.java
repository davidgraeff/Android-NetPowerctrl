package oly.netpowerctrl.utils;

import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

/**
 * Use this helper class to add a "done" and "cancel"
 * actions to the action bar and change the title of the action bar
 * if necessary.
 */
public class ActionBarTitle {
    private CharSequence title_before;
    private CharSequence subtitle_before;

    public void setTitle(Activity activity, int res) {
        title_before = activity.getTitle();
        activity.setTitle(res);
    }

    public void setTitle(Activity activity, String title) {
        title_before = activity.getTitle();
        activity.setTitle(title);
    }

    public void restoreTitle(ActionBarActivity activity) {
        if (title_before != null)
            activity.setTitle(title_before);
        if (subtitle_before != null)
            activity.getSupportActionBar().setSubtitle(title_before);
    }

    public void setSubTitle(ActionBarActivity activity, String string) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            subtitle_before = actionBar.getSubtitle();
            actionBar.setSubtitle(string);
        }
    }
}
