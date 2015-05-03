package oly.netpowerctrl.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * An android widget provider for "Executables" (Scenes or outlet actions) on the home screen.
 * Delegates all work to the {@link WidgetUpdateService} class.
 */
public class ProviderExecutable extends AppWidgetProvider {

    static String getTypeString() {
        return ProviderExecutable.class.getName();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            SharedPrefs.getInstance().deleteWidget(appWidgetId, getTypeString());
        }
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_COMMAND, WidgetUpdateService.DELETE_WIDGET);
        intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_TYPE, getTypeString());
        context.startService(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_COMMAND, WidgetUpdateService.UPDATE_WIDGET);
        intent.putExtra(WidgetUpdateService.EXTRA_WIDGET_TYPE, getTypeString());
        // Update the widgets via the service
        context.startService(intent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context.getApplicationContext(), WidgetUpdateService.class));
    }
}
