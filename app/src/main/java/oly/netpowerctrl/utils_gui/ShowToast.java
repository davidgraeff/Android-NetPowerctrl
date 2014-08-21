package oly.netpowerctrl.utils_gui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import org.acra.ACRA;

/**
 * Show a toast message, especially if not within the main thread
 * (Service-, Socket Thread).
 */
public class ShowToast {
    public static void FromOtherThread(final Context ctx, final String message) {
        Handler h = new Handler(ctx.getMainLooper());

        h.post(new Runnable() {
            public void run() {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void FromOtherThread(final Context ctx, final int resID) {
        Handler h = new Handler(ctx.getMainLooper());

        h.post(new Runnable() {
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

    public static void showDialogFragment(Activity context, Fragment fragment) {
        FragmentManager fragmentManager = context.getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ((DialogFragment) fragment).show(ft, "dialog");
    }

    public static void showException(Context context, String message) {
        FromOtherThread(context, message);
        ACRA.getErrorReporter().putCustomData("misc", message);
        ACRA.getErrorReporter().handleException(new Exception(message), false);
    }
}
