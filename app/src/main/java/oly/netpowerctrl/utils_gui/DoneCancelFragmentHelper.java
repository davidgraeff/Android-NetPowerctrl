package oly.netpowerctrl.utils_gui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;

/**
 * Use this helper class for any fragment to add a "done" and "cancel"
 * action to the action bar and change the title of the action bar
 * if necessary.
 */
public class DoneCancelFragmentHelper {
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

    public void addCancelDone(Activity activity, int res) {
        //set the actionbar to use the custom view (can also be done with a style)
        @SuppressLint("AppCompatMethod") ActionBar bar = activity.getActionBar();
        assert bar != null;
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setHomeButtonEnabled(true);
        bar.setCustomView(res);
    }

    public void restoreActionBar(Activity activity) {
        @SuppressLint("AppCompatMethod") ActionBar bar = activity.getActionBar();
        assert bar != null;
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);
    }
}
