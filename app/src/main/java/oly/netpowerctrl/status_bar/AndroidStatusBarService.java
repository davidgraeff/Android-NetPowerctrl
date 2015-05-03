package oly.netpowerctrl.status_bar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Show a permanent notification in the android statusbar and add favourite scenes and devicePorts as actions.
 */
public class AndroidStatusBarService extends Service implements onServiceReady {

    public static AndroidStatusBarService instance;
    NotificationManager mNotificationManager;
    private onCollectionUpdated<FavCollection, FavItem> collectionUpdateListener =
            new onCollectionUpdated<FavCollection, FavItem>() {
                @Override
                public boolean updated(@NonNull FavCollection collection, FavItem item, @NonNull ObserverUpdateActions action) {
                    createNotification(collection.dataService);
                    return true;
                }
            };

    public static void startOrStop(Context context) {
        if (SharedPrefs.isNotification(context))
            context.startService(new Intent(context, AndroidStatusBarService.class));
        else
            context.stopService(new Intent(context, AndroidStatusBarService.class));
    }

    private void createNotification(DataService dataService) {
        Context context = this;
        Intent startMainIntent = new Intent(context, MainActivity.class);
        startMainIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent startMainPendingIntent =
                PendingIntent.getActivity(context, (int) System.currentTimeMillis(), startMainIntent, 0);

        Notification.Builder b;

        FavCollection g = dataService.favourites;
        int maxLength = 0;
        List<FavItem> items = new ArrayList<>(g.getItems().values());

        if (items.size() == 0) {
            b = new Notification.Builder(context)
                    .setContentText(context.getString(R.string.statusbar_no_favourites))
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.netpowerctrl)
                    .setContentIntent(startMainPendingIntent)
                    .setOngoing(true);
        } else {
            RemoteViews remoteViewsRoot = new RemoteViews(context.getPackageName(), R.layout.statusbar_container);

            for (FavItem favItem : items) {
                Executable executable = dataService.executables.findByUID(favItem.getUid());

                if (executable == null) {
                    // No executable found for the given uid. We remove the item from the favourites now.
                    g.setFavourite(favItem.getUid(), false);
                    continue;
                }

                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = AndroidShortcuts.createShortcutExecutionIntent(context, favItem.getUid(), false, true);
                if (clickIntent == null)
                    continue;
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

                Bitmap bitmap = LoadStoreIconData.loadBitmap(context, executable, LoadStoreIconData.IconState.OnlyOneState, null);
                RemoteViews remoteViewsWidget = new RemoteViews(context.getPackageName(), R.layout.widget_executable);
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

        mNotificationManager.cancel(1);
        //noinspection deprecation
        mNotificationManager.notify(1, b.build());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        DataService.observersServiceReady.register(this);
    }

    @Override
    public void onDestroy() {
        instance = null;
        mNotificationManager.cancel(1);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onServiceReady(DataService service) {
        createNotification(service);
        service.favourites.registerObserver(collectionUpdateListener);
        return true;
    }

    @Override
    public void onServiceFinished(DataService service) {

    }
}
