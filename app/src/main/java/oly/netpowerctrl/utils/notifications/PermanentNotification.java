package oly.netpowerctrl.utils.notifications;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by david on 19.09.14.
 */
abstract public class PermanentNotification {
    protected String id;

    public PermanentNotification(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    abstract public View getView(Activity context, ViewGroup parent);
}
