package oly.netpowerctrl.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import org.acra.ACRA;

import oly.netpowerctrl.main.App;

/**
 * Show a toast message, especially if not within the main thread
 * (Service-, Socket Thread).
 */
public class ShowToast {
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

    public static void showToast(final Context ctx, final String message, int length) {
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
}
