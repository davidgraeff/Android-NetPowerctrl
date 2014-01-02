package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.SparseArray;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collection;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommand;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.ShortcutExecutionActivity;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements DeviceUpdateStateOrTimeout {
    private static final String LOG = "WidgetUpdateService";
    private AppWidgetManager appWidgetManager;
    private boolean listener_started = false;

    public static class DeviceInfoOutletNumber {
        public final DeviceInfo di;
        public final int outletNumber;

        public DeviceInfoOutletNumber(DeviceInfo di, int outletNumber) {
            this.di = di;
            this.outletNumber = outletNumber;
        }
    }

    private SparseArray<DeviceInfoOutletNumber> widgetUpdateRequests = new SparseArray<DeviceInfoOutletNumber>();

    @Override
    public void onDestroy() {
        if (listener_started)
            NetpowerctrlApplication.instance.stopListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        //noinspection ConstantConditions
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

        // Extract widget ids from intent
        int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        assert allWidgetIds != null;

        // Filter out widget ids not for DeviceWidgetProvider
        ComponentName thisWidget = new ComponentName(getApplicationContext(),
                DeviceWidgetProvider.class);
        int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);

        // Exit if no ids remain
        if (allWidgetIds2.length == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        //Log.w(LOG, "From Intent " + String.valueOf(allWidgetIds.length));
        //Log.w(LOG, "Direct " + String.valueOf(allWidgetIds2.length));

        boolean noFetch = false;
        if (intent.getExtras() != null)
            noFetch = intent.getExtras().containsKey("noFetch");

        // Keep a list of devices to fetch state updates
        Collection<DeviceInfo> devices_to_update = new ArrayList<DeviceInfo>();

        // For all widget ids, update the corresponding device first
        for (int appWidgetId : allWidgetIds2) {
            SharedPrefs.UniqueOutlet outlet = SharedPrefs.LoadWidget(getApplicationContext(), appWidgetId);
            DeviceInfo di = null;
            if (outlet != null) {
                di = NetpowerctrlApplication.instance.findDevice(outlet.deviceMac);
            }

            if (outlet == null || di == null) {
                setWidgetStateUnknown(appWidgetId);
                continue;
            }

            // Add to waiting widgets
            widgetUpdateRequests.append(appWidgetId, new DeviceInfoOutletNumber(di, outlet.outletNumber));

            devices_to_update.add(di);
        }

        if (devices_to_update.isEmpty())
            return START_NOT_STICKY;

        if (noFetch) {
            for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
                int appWidgetId = widgetUpdateRequests.keyAt(i);
                DeviceInfoOutletNumber widget_di = widgetUpdateRequests.get(appWidgetId);
                widgetUpdateRequests.removeAt(i);
                OutletInfo oi = widget_di.di.findOutlet(widget_di.outletNumber);
                if (oi != null)
                    setWidgetState(appWidgetId, OutletCommand.fromOutletInfo(oi, true));
                else
                    setWidgetStateUnknown(appWidgetId);
                break;
            }

            finishServiceIfDone();
        } else {
            listener_started = true;
            NetpowerctrlApplication.instance.startListener();
            NetpowerctrlApplication.instance.updateDeviceState(this, devices_to_update);
        }

        return START_STICKY;
    }

    public static void updateWidgetWithoutDataFetch(int appWidgetId, Context context) {
        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(), WidgetUpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        intent.putExtra("noFetch", true);

        // Update the widgets via the service
        context.startService(intent);
    }

    private void setWidgetStateUnknown(int appWidgetId) {
        @SuppressWarnings("ConstantConditions")
        RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
        views.setImageViewResource(R.id.widget_image, R.drawable.widgetunknown);
        views.setTextViewText(R.id.widget_name, "Broken");

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setWidgetState(int appWidgetId, OutletCommand c) {
        c.state = 2; // switch outlet
        OutletInfo oi = c.outletinfo;
        OutletCommandGroup og = new OutletCommandGroup();
        og.add(c);

        Context context = getApplicationContext();
        assert context != null;

//			DeviceInfo myDevice = SharedPrefs.ReadDevice(context, SharedPrefs.PREF_WIDGET_BASENAME+String.valueOf(appWidgetId));
        Intent clickIntent = new Intent(context, ShortcutExecutionActivity.class);
        clickIntent.putExtra("commands", og.toString());
        clickIntent.putExtra("widgetId", appWidgetId);
        clickIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setImageViewResource(R.id.widget_image, oi.State ? R.drawable.widgeton : R.drawable.widgetoff);
        views.setTextViewText(R.id.widget_name, oi.UserDescription.isEmpty() ? oi.Description : oi.UserDescription);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static public void ForceUpdate(Context ctx, int widgetId) {
        Intent updateWidget = new Intent();
        updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
        ctx.sendBroadcast(updateWidget);
    }

    @SuppressWarnings("unused")
    static public void ForceUpdateAll(Context ctx) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        ComponentName thisAppWidget = new ComponentName(ctx.getPackageName(), DeviceWidgetProvider.class.getName());

        @SuppressWarnings("ConstantConditions")
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        assert appWidgetIds != null;
        for (int id : appWidgetIds) {
            ForceUpdate(ctx, id);
        }
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
            int appWidgetId = widgetUpdateRequests.keyAt(i);
            widgetUpdateRequests.removeAt(i);
            setWidgetStateUnknown(appWidgetId);
        }
        stopSelf();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
            int appWidgetId = widgetUpdateRequests.keyAt(i);
            DeviceInfoOutletNumber widget_di = widgetUpdateRequests.get(appWidgetId);
            if (widget_di.di.equals(di)) {
                widgetUpdateRequests.removeAt(i);
                OutletInfo oi = di.findOutlet(widget_di.outletNumber);
                if (oi != null)
                    setWidgetState(appWidgetId, OutletCommand.fromOutletInfo(oi, true));
                else
                    setWidgetStateUnknown(appWidgetId);
                break;
            }
        }

        finishServiceIfDone();
    }

    private void finishServiceIfDone() {
        if (widgetUpdateRequests.size() == 0) {
            stopSelf();
        }
    }
}
