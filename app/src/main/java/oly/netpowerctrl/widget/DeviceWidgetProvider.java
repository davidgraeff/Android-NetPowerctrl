package oly.netpowerctrl.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import oly.netpowerctrl.data.SharedPrefs;

public class DeviceWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_WIDGET_IDS = "widget_ids";
    public static final String EXTRA_WIDGET_COMMAND = "command";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            SharedPrefs.getInstance().DeleteWidgets(appWidgetId);
        }
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds);
        intent.putExtra(EXTRA_WIDGET_COMMAND, WidgetUpdateService.DELETE_WIDGET);
        context.startService(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds);
        intent.putExtra(EXTRA_WIDGET_COMMAND, WidgetUpdateService.UPDATE_WIDGET);
        // Update the widgets via the service
        context.startService(intent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context.getApplicationContext(), WidgetUpdateService.class));
    }
}
