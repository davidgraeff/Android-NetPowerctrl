package oly.netpowerctrl.service;

import oly.netpowerctrl.DeviceInfo;
import oly.netpowerctrl.R;
import oly.netpowerctrl.main.DeviceControlActivity;
import oly.netpowerctrl.utils.SharedPrefs;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class DeviceWidgetProvider extends AppWidgetProvider {
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i=0; i<appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

			DeviceInfo myDevice = SharedPrefs.ReadDevice(context, SharedPrefs.PREF_WIDGET_BASENAME+String.valueOf(appWidgetId));
            Intent intent = new Intent(context, DeviceControlActivity.class);
            intent.putExtra("device", myDevice);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            if (myDevice != null)
            	views.setTextViewText(R.id.widget_name, myDevice.DeviceName);
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
