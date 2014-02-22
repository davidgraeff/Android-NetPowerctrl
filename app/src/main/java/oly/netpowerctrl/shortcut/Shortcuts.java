package oly.netpowerctrl.shortcut;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

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
        Intent shortcutIntent = new Intent(context, ExecutionActivity.class);
        shortcutIntent.putExtra(EditShortcutActivity.RESULT_SCENE, og.toJSON());

        if (show_mainWindow) {
            shortcutIntent.putExtra("show_mainWindow", true);
        }

        if (enable_feedback) {
            shortcutIntent.putExtra("enable_feedback", true);
        }

        return shortcutIntent;
    }


    public static Intent createShortcutExecutionIntent(Context context,
                                                       Scene.SceneItem item,
                                                       boolean show_mainWindow,
                                                       boolean enable_feedback) {
        // Create shortcut intent
        Intent shortcutIntent = new Intent(context, ExecutionActivity.class);
        shortcutIntent.putExtra(EditShortcutActivity.RESULT_ACTION_UUID, item.uuid.toString());
        shortcutIntent.putExtra(EditShortcutActivity.RESULT_ACTION_COMMAND, item.command);

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

    static public Intent createShortcut(Intent extra, String name, Bitmap icon) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, extra);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

        return intent;
    }

}
