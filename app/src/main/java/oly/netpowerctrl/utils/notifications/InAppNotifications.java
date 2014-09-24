package oly.netpowerctrl.utils.notifications;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;

/**
 * Show a toast message, especially if not within the main thread
 * (Service-, Socket Thread).
 */
public class InAppNotifications {
    private static List<PermanentNotification> permanentNotifications = new ArrayList<>();

    public static void FromOtherThread(final Context ctx, final String message) {
        App.getMainThreadHandler().post(new Runnable() {
            public void run() {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void FromOtherThread(final Context ctx, final int resID) {
        App.getMainThreadHandler().post(new Runnable() {
            public void run() {
                Toast.makeText(ctx, resID, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void toastWithLength(final Context ctx, final String message, int length) {
        final Toast toast = Toast.makeText(ctx, message, Toast.LENGTH_LONG);
        toast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, length);
    }

    public static void showException(Context context, String message) {
        FromOtherThread(context, message);
        ACRA.getErrorReporter().putCustomData("misc", message);
        ACRA.getErrorReporter().handleException(new Exception(message), false);
    }

    public static void showException(Context context, Exception exception, String message) {
        FromOtherThread(context, message);
        ACRA.getErrorReporter().putCustomData("misc", message);
        ACRA.getErrorReporter().handleException(exception, false);
    }

    public static void addPermanentNotification(Activity activity, PermanentNotification permanentNotification) {
        permanentNotifications.add(permanentNotification);

        showPermanentNotifications(activity);
    }

    public static void removePermanentNotification(Activity activity, String id) {
        Iterator<PermanentNotification> it = permanentNotifications.iterator();
        while (it.hasNext())
            if (it.next().getID().equals(id))
                it.remove();

        if (activity == null)
            return;

        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.notifications);
        if (layout == null) {
            return;
        }

        View v = layout.findViewWithTag(id);
        if (v != null) {
            layout.removeView(v);
        }
    }

    public static void showPermanentNotifications(Activity activity) {
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.notifications);
        if (layout == null) {
            return;
        }

        for (PermanentNotification permanentNotification : permanentNotifications) {
            View v = layout.findViewWithTag(permanentNotification.getID());
            if (v != null) {
                layout.removeView(v);
            }
            v = permanentNotification.getView(activity, layout);
            v.setTag(permanentNotification.getID());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layout.addView(v, lp);

        }
    }
}
