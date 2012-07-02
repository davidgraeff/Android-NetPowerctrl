package oly.netpowerctrl;

import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class DeviceWidgetProvider extends AppWidgetProvider {
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		SharedPreferences prefs = context.getSharedPreferences("oly.netpowerctrl.widgets", Context.MODE_PRIVATE);
		List<DeviceInfo> devices = SharedPrefs.ReadConfiguredDevices(context);
		
		final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

			String myuuid = prefs.getString(String.format("%08x", appWidgetId), "");
			
			DeviceInfo myDevice = null;
			for (DeviceInfo di: devices) {
				if (di.uuid.toString().equals(myuuid)) {
					myDevice = di;
					break;
				}
			}
            
            Intent intent = new Intent(context, DeviceControl.class);
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
