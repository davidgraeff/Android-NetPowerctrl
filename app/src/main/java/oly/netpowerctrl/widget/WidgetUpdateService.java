package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collection;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceUpdate;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.anelservice.NetpowerctrlService;
import oly.netpowerctrl.anelservice.ServiceReady;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.ShortcutExecutionActivity;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements DeviceUpdateStateOrTimeout, DeviceUpdate, ServiceReady, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG = "WidgetUpdateService";
    private AppWidgetManager appWidgetManager;
    private boolean listener_started = false;
    private boolean keep_service_online;

    public static class DeviceInfoOutletNumber {
        public final DeviceInfo di;
        public final int outletNumber;

        public DeviceInfoOutletNumber(DeviceInfo di, int outletNumber) {
            this.di = di;
            this.outletNumber = outletNumber;
        }
    }

    private SparseArray<DeviceInfoOutletNumber> widgetUpdateRequests = new SparseArray<DeviceInfoOutletNumber>();
    private SparseArray<DeviceInfoOutletNumber> allWidgets = new SparseArray<DeviceInfoOutletNumber>();

    @Override
    public void onDestroy() {
        if (listener_started)
            NetpowerctrlApplication.instance.stopListener();

        /**
         * If the service is kept running but now should be finished (preferences changed,
         * system ends service) unregister from the listener service and from the shared preferences
         * changed signal.
         */
        if (keep_service_online) {
            NetpowerctrlService service = NetpowerctrlApplication.instance.getService();
            if (service != null)
                service.unregisterDeviceUpdateObserver(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onCreate() {
        //noinspection ConstantConditions
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

        /**
         * If the service is kept running, we will receive further device updates
         * and register on the listener service for this purpose.
         */
        keep_service_online = SharedPrefs.getKeepWidgetServiceOn(getApplicationContext());
        if (keep_service_online) {
            NetpowerctrlApplication.instance.registerServiceReadyObserver(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        super.onCreate();
    }

    /**
     * This method will be called by every startService call. If the android system
     * requested a widget update, this is called for example.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Extract widget ids from intent
        //int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        //assert allWidgetIds != null;

        // Only update widgets belonging to DeviceWidgetProvider
        ComponentName thisWidget = new ComponentName(getApplicationContext(),
                DeviceWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Exit if no widgets
        if (allWidgetIds.length == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean noFetch = false;
        if (intent != null && intent.getExtras() != null)
            noFetch = intent.getExtras().containsKey("noFetch");

        // Keep a list of scenes to fetch state updates
        Collection<DeviceInfo> devices_to_update = new ArrayList<DeviceInfo>();

        // clear all widgets list
        allWidgets.clear();

        // For all widget ids, update the corresponding device first
        for (int appWidgetId : allWidgetIds) {
            SharedPrefs.WidgetOutlet outlet = SharedPrefs.LoadWidget(getApplicationContext(), appWidgetId);
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
            allWidgets.append(appWidgetId, new DeviceInfoOutletNumber(di, outlet.outletNumber));

            devices_to_update.add(di);
        }

        if (devices_to_update.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (noFetch) {
            for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
                int appWidgetId = widgetUpdateRequests.keyAt(i);
                DeviceInfoOutletNumber widget_di = widgetUpdateRequests.get(appWidgetId);
                widgetUpdateRequests.removeAt(i);
                OutletInfo oi = widget_di.di.findOutlet(widget_di.outletNumber);
                if (oi != null)
                    setWidgetState(appWidgetId, SceneOutlet.fromOutletInfo(oi, true));
                else
                    setWidgetStateUnknown(appWidgetId);
            }

            finishServiceIfDone();
        } else {
            listener_started = true;
            NetpowerctrlApplication.instance.startListener(false);
            new DeviceQuery(this, this, devices_to_update, false);
        }

        return START_STICKY;
    }

    /**
     * Start this widget update service (or if it is running, an update iteration),
     * but do not fetch the current states of the outlets before updating the widget states
     *
     * @param appWidgetId
     * @param context
     */
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

    private void setWidgetState(int appWidgetId, SceneOutlet c) {
        c.state = 2; // switch outlet
        OutletInfo oi = c.outletinfo;
        Scene og = new Scene();
        og.add(c);

        Context context = getApplicationContext();
        assert context != null;

        // This intent will be executed by a click on the widget
        Intent clickIntent = new Intent(context, ShortcutExecutionActivity.class);
        clickIntent.putExtra("commands", og.toString());
        clickIntent.putExtra("widgetId", appWidgetId);
        clickIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setImageViewResource(R.id.widget_image, oi.State ? R.drawable.widgeton : R.drawable.widgetoff);
        views.setTextViewText(R.id.widget_name, oi.getDescription());
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
        finishServiceIfDone();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
            int appWidgetId = widgetUpdateRequests.keyAt(i);
            DeviceInfoOutletNumber widget_di = widgetUpdateRequests.get(appWidgetId);
            if (widget_di.di.equalsFuntional(di)) {
                widgetUpdateRequests.removeAt(i);
                OutletInfo oi = di.findOutlet(widget_di.outletNumber);
                if (oi != null)
                    setWidgetState(appWidgetId, SceneOutlet.fromOutletInfo(oi, true));
                else
                    setWidgetStateUnknown(appWidgetId);

                finishServiceIfDone();
                return;
            }
        }

        /**
         * If the service is kept running, we will receive further device updates
         * and update the widgets here.
         */
        if (widgetUpdateRequests.size() == 0) {
            ComponentName thisWidget = new ComponentName(getApplicationContext(),
                    DeviceWidgetProvider.class);
            //noinspection ConstantConditions
            appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int appWidgetId : allWidgetIds) {
                DeviceInfoOutletNumber widget_di = allWidgets.get(appWidgetId);
                if (widget_di.di.equalsFuntional(di)) {
                    OutletInfo oi = widget_di.di.findOutlet(widget_di.outletNumber);
                    if (oi != null)
                        setWidgetState(appWidgetId, SceneOutlet.fromOutletInfo(oi, true));
                    else
                        setWidgetStateUnknown(appWidgetId);
                    break;
                }
            }
        }

        finishServiceIfDone();
    }

    @Override
    public void onDeviceQueryFinished(int timeout_devices) {

    }

    @Override
    public void onServiceReady(NetpowerctrlService mDiscoverService) {
        mDiscoverService.registerDeviceUpdateObserver(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        keep_service_online = sharedPreferences.getBoolean(key, false);
        finishServiceIfDone();
    }

    private void finishServiceIfDone() {
        if (widgetUpdateRequests.size() == 0 && !keep_service_online) {
            stopSelf();
        }
    }
}
