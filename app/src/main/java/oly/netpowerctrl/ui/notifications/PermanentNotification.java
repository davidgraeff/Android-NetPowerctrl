package oly.netpowerctrl.ui.notifications;

import android.app.Activity;

import com.rey.material.widget.SnackBar;

import java.lang.ref.WeakReference;

/**
 * Created by david on 19.09.14.
 */
abstract public class PermanentNotification {
    protected final WeakReference<Activity> activityWeakReference;
    protected final String id;

    public PermanentNotification(String id, Activity activity) {
        this.id = id;
        activityWeakReference = new WeakReference<>(activity);
    }

    public String getID() {
        return id;
    }

    abstract public String getText();

    abstract public boolean hasCloseButton();

    abstract public void onDismiss();

    public void action(SnackBar snackBar) {
    }

    public String getActionButtonText() {
        return null;
    }
}
