package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletCommand;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.ShortcutExecutionActivity;

public class DeviceWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            SharedPrefs.UniqueOutlet outlet = SharedPrefs.LoadWidget(context, appWidgetId);
            if (outlet == null) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
                views.setImageViewResource(R.id.widget_image, R.drawable.widgetunknown);
                views.setTextViewText(R.id.widget_name, "Broken");

                appWidgetManager.updateAppWidget(appWidgetId, views);
                continue;
            }

            OutletInfo oi = NetpowerctrlApplication.instance.findOutlet(outlet.deviceMac, outlet.outletNumber);

            OutletCommandGroup og = new OutletCommandGroup();
            OutletCommand c = new OutletCommand();
            c.device_mac = outlet.deviceMac;
            c.outletNumber = outlet.outletNumber;
            c.state = 2;
            og.add(c);

//			DeviceInfo myDevice = SharedPrefs.ReadDevice(context, SharedPrefs.PREF_WIDGET_BASENAME+String.valueOf(appWidgetId));
            Intent intent = new Intent(context, ShortcutExecutionActivity.class);
            intent.putExtra("commands", og.toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setImageViewResource(R.id.widget_image, oi.State ? R.drawable.widgeton : R.drawable.widgetoff);
            views.setTextViewText(R.id.widget_name, oi.UserDescription.isEmpty() ? oi.Description : oi.UserDescription);
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
