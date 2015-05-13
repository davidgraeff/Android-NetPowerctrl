package oly.netpowerctrl.scenes;

import android.content.Context;
import android.view.View;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;

import oly.netpowerctrl.R;

/**
 * Show help for scene dialog
 */
public class SceneHelp {
    public static void showHelp(Context context, int title_res, int help_res) {
        final SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight);
        builder.title(context.getString(title_res));
        builder.message(context.getString(help_res));
        builder.positiveAction(context.getString(android.R.string.ok));
        final Dialog dialog = builder.build(context);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
