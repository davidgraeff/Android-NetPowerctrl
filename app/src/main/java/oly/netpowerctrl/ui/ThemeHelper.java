package oly.netpowerctrl.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import oly.netpowerctrl.R;

/**
 * Theme helper methods
 */
public class ThemeHelper {
    public static int getDialogRes(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.dialogTheme, typedValue, true);
        return typedValue.resourceId;
    }
}
