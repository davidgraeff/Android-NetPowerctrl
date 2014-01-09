package oly.netpowerctrl.shortcut;

import android.content.Context;
import android.content.Intent;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;

/**
 * Shortcut Utility class
 */
public class Shortcuts {
    static public Intent createShortcutExecutionIntent(Context context,
                                                       Scene og,
                                                       boolean show_mainWindow,
                                                       boolean enable_feedback) {
        if (og.length() == 0) {
            return null;
        }

        // Create shortcut intent
        Intent shortcutIntent = new Intent(context, ShortcutExecutionActivity.class);
        shortcutIntent.putExtra(ShortcutCreatorActivity.RESULT_SCENE, og.toJSON());
        shortcutIntent.setClass(context, ShortcutExecutionActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        if (show_mainWindow) {
            shortcutIntent.putExtra("show_mainWindow", true);
        }

        if (enable_feedback) {
            shortcutIntent.putExtra("enable_feedback", true);
        }

        return shortcutIntent;
    }

    static public Intent createShortcut(Context context, Intent extra, String name) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, extra);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.drawable.netpowerctrl));

        return intent;
    }
}
