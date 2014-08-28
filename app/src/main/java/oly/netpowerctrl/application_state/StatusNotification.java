package oly.netpowerctrl.application_state;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.utils.Shortcuts;

/**
 * Created by david on 08.07.14.
 */
public class StatusNotification {
    public static void update(Context context) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager == null)
            return;

        if (!SharedPrefs.getInstance().isNotification()) {
//            Log.w("r","remove");
            mNotificationManager.cancel(1);
            return;
        }

        Intent startMainIntent = new Intent(context, MainActivity.class);
        startMainIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent startMainPendingIntent =
                PendingIntent.getActivity(context, (int) System.currentTimeMillis(), startMainIntent, 0);

        Notification.Builder b = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.app_name))
                .setSmallIcon(R.drawable.netpowerctrl)
                .setContentIntent(startMainPendingIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            SceneCollection g = RuntimeDataController.getDataController().sceneCollection;
            int maxLength = 0;
            for (Scene scene : g.scenes) {
                if (!scene.isFavourite())
                    continue;
                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = Shortcuts.createShortcutExecutionIntent(context, scene, false, true);
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

                b.addAction(0, scene.sceneName, pendingIntent);
            }
        }

        mNotificationManager.notify(1, b.getNotification());
    }

    public static void init(final Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//                    Log.w("changed", s);
                if (s.equals(SharedPrefs.getInstance().PREF_show_persistent_notification))
                    StatusNotification.update(context);
            }
        });
        RuntimeDataController.getDataController().sceneCollection.registerObserver(new SceneCollection.IScenesUpdated() {
            @Override
            public void scenesUpdated(boolean addedOrRemoved) {
                StatusNotification.update(context);
            }
        });
        StatusNotification.update(context);
    }
}
