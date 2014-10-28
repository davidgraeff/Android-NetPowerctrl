package oly.netpowerctrl.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;

/**
 * Created by david on 08.07.14.
 */
public class AndroidStatusBarNotification {
    private static onCollectionUpdated<SceneCollection, Scene> collectionUpdateListener = null;

    public AndroidStatusBarNotification(final Context context) {
        SharedPrefs.getInstance().registerShowPersistentNotification(new SharedPrefs.IShowPersistentNotification() {
            @Override
            public void showPersistentNotificationChanged(boolean enabled) {
                setEnabled(context, enabled);
            }
        });

        if (SharedPrefs.getInstance().isNotification())
            setEnabled(context, true);
    }

    private static Notification createNotification(Context context) {
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
            SceneCollection g = AppData.getInstance().sceneCollection;
            int maxLength = 0;
            for (Scene scene : g.getItems()) {
                if (!scene.isFavourite())
                    continue;
                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = AndroidShortcuts.createShortcutExecutionIntent(context, scene, false, true);
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

                b.addAction(0, scene.sceneName, pendingIntent);
            }
        }

        //noinspection deprecation
        return b.getNotification();
    }

    private static void setEnabled(final Context context, boolean enabled) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager == null)
            return;

        if (enabled) {
            if (collectionUpdateListener == null) {
                collectionUpdateListener = new onCollectionUpdated<SceneCollection, Scene>() {
                    @Override
                    public boolean updated(SceneCollection sceneCollection, Scene scene, ObserverUpdateActions action, int position) {
                        setEnabled(context, true);
                        return true;
                    }
                };
                AppData.getInstance().sceneCollection.registerObserver(collectionUpdateListener);
            }

            mNotificationManager.notify(1, createNotification(context));
        } else { // disabled
            if (collectionUpdateListener != null) {
                AppData.getInstance().sceneCollection.unregisterObserver(collectionUpdateListener);
                collectionUpdateListener = null;
            }
            mNotificationManager.cancel(1);
        }
    }

    public static void init(final Context context) {
        SharedPrefs.getInstance().registerShowPersistentNotification(new SharedPrefs.IShowPersistentNotification() {
            @Override
            public void showPersistentNotificationChanged(boolean enabled) {
                setEnabled(context, enabled);
            }
        });

        if (SharedPrefs.getInstance().isNotification())
            setEnabled(context, true);
    }
}
