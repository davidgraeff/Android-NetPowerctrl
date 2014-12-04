package oly.netpowerctrl.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.FavCollection;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.main.MainActivity;

/**
 * Show a permanent notification in the android statusbar and add favourite scenes and devicePorts as actions.
 */
public class AndroidStatusBarNotification {
    private static onCollectionUpdated<FavCollection, FavCollection.FavItem> collectionUpdateListener = null;

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

        Notification.Builder b;

        AppData appData = AppData.getInstance();
        FavCollection g = appData.favCollection;
        int maxLength = 0;
        List<FavCollection.FavItem> items = new ArrayList<>(g.getItems());

        if (items.size() == 0) {
            b = new Notification.Builder(context)
                    .setContentText(context.getString(R.string.statusbar_no_favourites))
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.netpowerctrl)
                    .setContentIntent(startMainPendingIntent)
                    .setOngoing(true);
        } else {
            RemoteViews remoteViewsRoot = new RemoteViews(context.getPackageName(), R.layout.statusbar_container);

            for (FavCollection.FavItem favItem : items) {
                Executable executable = appData.findExecutable(favItem.executable_uid);

                if (executable == null) {
                    // No executable found for the given uid. We remove the item from the favCollection now.
                    g.setFavourite(favItem.executable_uid, false);
                    continue;
                }

                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = AndroidShortcuts.createShortcutExecutionIntent(context, favItem.executable_uid, false, true);
                if (clickIntent == null)
                    continue;
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

                Bitmap bitmap = LoadStoreIconData.loadBitmap(context, executable.getUid(), LoadStoreIconData.IconState.OnlyOneState, null);
                RemoteViews remoteViewsWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
                remoteViewsWidget.setTextViewText(R.id.widget_name, executable.getTitle());
                remoteViewsWidget.setImageViewBitmap(R.id.widget_image, bitmap);
                remoteViewsWidget.setViewVisibility(R.id.widget_status, View.GONE);
                remoteViewsWidget.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
                remoteViewsRoot.addView(R.id.notifications, remoteViewsWidget);
            }

            b = new Notification.Builder(context)
                    .setContent(remoteViewsRoot)
                    .setSmallIcon(R.drawable.netpowerctrl)
                    .setContentIntent(startMainPendingIntent)
                    .setOngoing(true);
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
                collectionUpdateListener = new onCollectionUpdated<FavCollection, FavCollection.FavItem>() {
                    @Override
                    public boolean updated(@NonNull FavCollection collection, FavCollection.FavItem item, @NonNull ObserverUpdateActions action, int position) {
                        setEnabled(context, true);
                        return true;
                    }
                };
                AppData.getInstance().favCollection.registerObserver(collectionUpdateListener);
            }

            mNotificationManager.cancel(1);
            mNotificationManager.notify(1, createNotification(context));
        } else { // disabled
            if (collectionUpdateListener != null) {
                AppData.getInstance().favCollection.unregisterObserver(collectionUpdateListener);
                collectionUpdateListener = null;
            }
            mNotificationManager.cancel(1);
        }
    }
}
