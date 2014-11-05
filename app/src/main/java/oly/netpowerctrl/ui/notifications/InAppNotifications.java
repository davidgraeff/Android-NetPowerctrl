package oly.netpowerctrl.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.AnimationController;

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

    public static void showException(Context context, Throwable exception, String message) {
        FromOtherThread(context, message + "\n" + context.getString(R.string.error_restart_app));
        ACRA.getErrorReporter().putCustomData("misc", message);
        boolean serviceRunning = ListenService.getService() != null;
        ACRA.getErrorReporter().putCustomData("service_state", serviceRunning ? "running" : "down");
        if (serviceRunning) {
            ACRA.getErrorReporter().putCustomData("plugin_anel_state", ListenService.getService().getPluginByID(AnelPlugin.PLUGIN_ID).isNetworkReducedState() ? "down" : "running");
        } else {
            ACRA.getErrorReporter().putCustomData("service_shutdown_reason", ListenService.service_shutdown_reason);
        }

        if (exception == null)
            App.setErrorReportContentMessage();
        ACRA.getErrorReporter().handleException(exception, false);
        if (exception == null)
            App.setErrorReportContentCrash();
    }

    public static void silentException(Throwable exception) {
        ACRA.getErrorReporter().handleSilentException(exception);
    }

    public static void updatePermanentNotification(Activity activity, PermanentNotification newPermanentNotification) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_bottom_actionbar);
        if (toolbar == null) {
            return;
        }

        // If toolbar is visible -> there is still a notification shown. Add this new one to a backlog list.
        if (toolbar.getVisibility() == View.VISIBLE) {
            if (newPermanentNotification != null)
                permanentNotifications.add(newPermanentNotification);
            return;
        } else if (permanentNotifications.size() == 0 && newPermanentNotification == null)
            return;

        AnimationController.animateBottomViewIn(toolbar);

        doUpdatePermanentNotification(activity, toolbar, newPermanentNotification);
    }

    private static void doUpdatePermanentNotification(Activity activity, Toolbar toolbar, PermanentNotification newPermanentNotification) {
        View v = toolbar.findViewWithTag(newPermanentNotification.getID());
        if (v != null) {
            toolbar.removeView(v);
        }
        v = newPermanentNotification.getView(activity, toolbar);
        v.setTag(newPermanentNotification.getID());
        Toolbar.LayoutParams lp = new Toolbar.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        toolbar.addView(v, lp);
    }

    public static void closePermanentNotification(Activity activity, String id) {
        Iterator<PermanentNotification> it = permanentNotifications.iterator();
        while (it.hasNext())
            if (it.next().getID().equals(id))
                it.remove();

        if (activity == null)
            return;

        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_bottom_actionbar);
        if (toolbar == null) {
            return;
        }

        AnimationController.animateBottomViewOut(toolbar);

        View v = toolbar.findViewWithTag(id);
        if (v != null) {
            toolbar.removeView(v);
            if (permanentNotifications.size() > 0) {
                it = permanentNotifications.iterator();
                doUpdatePermanentNotification(activity, toolbar, it.next());
                it.remove();
            }
        }
    }
}
