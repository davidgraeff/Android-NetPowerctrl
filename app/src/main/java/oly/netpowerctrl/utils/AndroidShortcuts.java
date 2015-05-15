package oly.netpowerctrl.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.IconState;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.graphic.Utils;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.main.ExecutionActivity;

/**
 * Shortcut Utility class
 */
public class AndroidShortcuts {
    @Nullable
    static public Intent createExecutionCopyIntent(Context context,
                                                   Executable executable,
                                                   boolean show_mainWindow,
                                                   boolean enable_feedback) {
        // Create shortcut intent
        Intent shortcutIntent = new Intent(context, ExecutionActivity.class);
        shortcutIntent.putExtra(ExecutionActivity.EXECUTE_SCENE_JSON, executable.toString());

        if (show_mainWindow) {
            shortcutIntent.putExtra("show_mainWindow", true);
        }

        if (enable_feedback) {
            shortcutIntent.putExtra("enable_feedback", true);
        }

        return shortcutIntent;
    }


    public static Intent createExecutionLinkIntent(Context context,
                                                   String executable_uid,
                                                   boolean show_mainWindow,
                                                   boolean enable_feedback) {
        // Create shortcut intent
        Intent shortcutIntent = new Intent(context, ExecutionActivity.class);
        shortcutIntent.putExtra(ExecutionActivity.EXECUTE_ACTION_UUID, executable_uid);
        shortcutIntent.putExtra(ExecutionActivity.EXECUTE_ACTION_COMMAND, Executable.TOGGLE);

        if (show_mainWindow) {
            shortcutIntent.putExtra("show_mainWindow", true);
        }

        if (enable_feedback) {
            shortcutIntent.putExtra("enable_feedback", true);
        }

        return shortcutIntent;
    }

    static public Intent createShortcut(Intent extra, String name, Context context) {
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

    public static void createHomeIcon(Context context, Executable executable, Intent extra) {
        Bitmap bitmap = LoadStoreIconData.loadBitmap(context, executable,
                IconState.OnlyOneState, null);
        Intent shortcutIntent;
        if (bitmap != null) {
            shortcutIntent = AndroidShortcuts.createShortcut(extra, executable.getTitle(),
                    Utils.resizeBitmap(context, bitmap));
        } else
            shortcutIntent = AndroidShortcuts.createShortcut(extra, executable.getTitle(), context);

        shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        assert context != null;
        context.sendBroadcast(shortcutIntent);
    }
}
