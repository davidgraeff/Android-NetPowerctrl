package oly.netpowerctrl.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.ExecutionActivity;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;

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
        shortcutIntent.putExtra(EditSceneActivity.RESULT_SCENE, og.toJSON());

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
        shortcutIntent.putExtra(EditSceneActivity.RESULT_ACTION_UUID, item.uuid.toString());
        shortcutIntent.putExtra(EditSceneActivity.RESULT_ACTION_COMMAND, item.command);

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

    public static void createHomeIcon(Context context, Scene scene) {
        Intent extra = Shortcuts.createShortcutExecutionIntent(context, scene, false, false);
        Bitmap bitmap = Icons.loadIcon(context, scene.uuid,
                Icons.IconType.SceneIcon, Icons.IconState.StateUnknown, 0);
        Intent shortcutIntent;
        if (bitmap != null) {
            shortcutIntent = Shortcuts.createShortcut(extra, scene.sceneName,
                    Icons.resizeBitmap(context, bitmap));
        } else
            shortcutIntent = Shortcuts.createShortcut(extra, scene.sceneName, context);

        shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        assert context != null;
        context.sendBroadcast(shortcutIntent);
    }
}
