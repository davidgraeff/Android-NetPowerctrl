package oly.netpowerctrl.shortcut;

import android.content.Context;
import android.content.Intent;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletCommandGroup;

/**
 * Shortcut Utility class
 */
public class Shortcuts {
    static public Intent createShortcutExecutionIntent(Context context,
                                                       OutletCommandGroup og,
                                                       boolean show_mainWindow) {
        if (og.length() == 0) {
            return null;
        }

        // Create shortcut intent
        Intent shortcutIntent = new Intent(context, ShortcutExecutionActivity.class);
        shortcutIntent.putExtra("commands", og.toString());
        shortcutIntent.setClass(context, ShortcutExecutionActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        if (show_mainWindow) {
            shortcutIntent.putExtra("show_mainWindow", true);
        }

        return shortcutIntent;
    }

    static public Intent createShortcut(Context context, Intent extra, String name) {
        // Return result
        // Shortcut name is "app_name (9)" where 9 is the amount of commands
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, extra);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.drawable.netpowerctrl));

        return intent;
    }
}
