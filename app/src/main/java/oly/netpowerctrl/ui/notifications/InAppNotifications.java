package oly.netpowerctrl.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.rey.material.widget.SnackBar;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.plugin_anel.AnelPlugin;

/**
 * Show a toast message, especially if not within the main thread
 * (Service-, Socket Thread).
 */
public class InAppNotifications {
    private static List<PermanentNotification> permanentNotifications = new ArrayList<>();
    private static SnackBar.OnStateChangeListener stateChangeListener = new SnackBar.OnStateChangeListener() {
        @Override
        public void onStateChange(SnackBar snackBar, int oldState, int newState) {
            if (newState == SnackBar.STATE_DISMISSED) {
                PermanentNotification current = (PermanentNotification) snackBar.getTag();
                current.onDismiss();
                if (permanentNotifications.isEmpty()) return;
                PermanentNotification n = permanentNotifications.get(0);
                permanentNotifications.remove(0);

                Activity activity = current.activityWeakReference.get();
                if (activity != null)
                    doUpdatePermanentNotification(activity, snackBar, n);
            }
        }
    };

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
//        if (!App.useErrorReporter)
//            return;
        ACRA.getErrorReporter().putCustomData("misc", message);
        boolean serviceRunning = DataService.getService() != null;
        ACRA.getErrorReporter().putCustomData("service_state", serviceRunning ? "running" : "down");
        if (serviceRunning) {
            ACRA.getErrorReporter().putCustomData("plugin_anel_state", DataService.getService().getPlugin(AnelPlugin.PLUGIN_ID).isStarted() ? "down" : "running");
        } else {
            ACRA.getErrorReporter().putCustomData("service_shutdown_reason", DataService.service_shutdown_reason);
        }

        if (exception == null)
            App.setErrorReportContentMessage();
        ACRA.getErrorReporter().handleException(exception, false);
        if (exception == null)
            App.setErrorReportContentCrash();
    }

    public static void silentException(Throwable exception, String additionalData) {
//        if (!App.useErrorReporter) return;

        if (additionalData != null)
            ACRA.getErrorReporter().putCustomData("data", additionalData);
        ACRA.getErrorReporter().handleSilentException(exception);
    }

    public static void updatePermanentNotification(@NonNull Activity activity, @NonNull PermanentNotification newPermanentNotification) {
        SnackBar toolbar = (SnackBar) activity.findViewById(R.id.toolbar_bottom_actionbar);
        if (toolbar == null) {
            throw new RuntimeException("No toolbar for notifications found!");
        }

        toolbar.stateChangeListener(stateChangeListener);

        // If toolbar is visible -> there is still a notification shown. Add this new one to a backlog list.
        if (toolbar.getState() == SnackBar.STATE_SHOWED) {
            permanentNotifications.add(newPermanentNotification);
            return;
        }

        doUpdatePermanentNotification(activity, toolbar, newPermanentNotification);
    }

    private static void doUpdatePermanentNotification(Activity activity, final SnackBar toolbar, final PermanentNotification newPermanentNotification) {
        if (newPermanentNotification.hasCloseButton())
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toolbar.dismiss();
                }
            });
        toolbar.text(newPermanentNotification.getText());
        String actionText = newPermanentNotification.getActionButtonText();
        if (actionText != null) {
            toolbar.actionText(actionText);
            toolbar.actionClickListener(new SnackBar.OnActionClickListener() {
                @Override
                public void onActionClick(SnackBar snackBar, int i) {
                    newPermanentNotification.action(snackBar);
                }
            });
        }
        toolbar.setTag(newPermanentNotification);
        toolbar.show();
    }

    public static void moveToastNextToView(Toast toast, Resources resources, View view, boolean anchorRightOnView) {
        // measure toast to center it relatively to the anchor view
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.UNSPECIFIED);
        toast.getView().measure(widthMeasureSpec, heightMeasureSpec);
        int toastWidth = toast.getView().getMeasuredWidth();

        int coordinates[] = new int[2];
        view.getLocationInWindow(coordinates);
        toast.setGravity(Gravity.START | Gravity.TOP | Gravity.CENTER_VERTICAL, (int) view.getX() - toastWidth, coordinates[1]);
    }
}
