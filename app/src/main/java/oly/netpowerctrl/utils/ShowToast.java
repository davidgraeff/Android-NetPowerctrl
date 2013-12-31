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

}
