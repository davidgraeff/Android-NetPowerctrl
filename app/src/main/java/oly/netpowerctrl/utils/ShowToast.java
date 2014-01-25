package oly.netpowerctrl.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

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
        final Toast toast = Toast.makeText(ctx, message, Toast.LENGTH_SHORT);
        toast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, length);
    }
}
