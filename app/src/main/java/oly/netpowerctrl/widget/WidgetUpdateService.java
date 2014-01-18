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
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;

import oly.netpowerctrl.R;
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
import oly.netpowerctrl.preferences.WidgetPreferenceFragment;
import oly.netpowerctrl.shortcut.ShortcutExecutionActivity;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements DeviceUpdateStateOrTimeout, DeviceUpdate, ServiceReady, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG = "WidgetUpdateService";
    public static final int UPDATE_WIDGET = 0;
    public static final int DELETE_WIDGET = 1;
    private AppWidgetManager appWidgetManager;
    private boolean keep_service_online;
    Context context;

    public static class DeviceInfoOutletNumber {
        public final DeviceInfo di;
        public final int outletNumber;
        public int lastState = -1;

        boolean isSameAsLastState(int newState) {
            boolean temp = (lastState != -1) ? newState == lastState : false;
            lastState = newState;
            return temp;
        }

        public DeviceInfoOutletNumber(DeviceInfo di, int outletNumber) {
            this.di = di;
            this.outletNumber = outletNumber;
        }
    }

    private SparseArray<DeviceInfoOutletNumber> widgetUpdateRequests = new SparseArray<DeviceInfoOutletNumber>();
    private SparseArray<DeviceInfoOutletNumber> allWidgets = new SparseArray<DeviceInfoOutletNumber>();

    @Override
    public void onDestroy() {
        NetpowerctrlApplication.instance.stopUseListener();

        /**
         * If the service is kept running but now should be finished (preferences changed,
         * system ends service) unregister from the listener service and from the shared preferences
         * changed signal.
         */
        if (keep_service_online) {
            NetpowerctrlService service = NetpowerctrlApplication.instance.getService();
            if (service != null)
                service.unregisterDeviceUpdateObserver(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onCreate() {
        //noinspection ConstantConditions
        context = getApplicationContext();
        appWidgetManager = AppWidgetManager.getInstance(context);
        NetpowerctrlApplication.instance.useListener();

        /**
         * If the service is kept running, we will receive further device updates
         * and register on the listener service for this purpose.
         */
        keep_service_online = SharedPrefs.getKeepWidgetServiceOn(context);
        if (keep_service_online) {
            NetpowerctrlApplication.instance.registerServiceReadyObserver(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        super.onCreate();
    }

    private int[] getAllWidgetIDs() {
        // Only update widgets belonging to DeviceWidgetProvider
        ComponentName thisWidget = new ComponentName(context,
                DeviceWidgetProvider.class);
        return appWidgetManager.getAppWidgetIds(thisWidget);
    }

    /**
     * This method will be called by every startService call.
     * We update a widget here, if the android system requested a widget update.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w("WidgetUpdateService", "No intent! onStartCommand");
            return START_NOT_STICKY;
        }
        // Extract widget ids from intent
        final int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        int command = intent.getIntExtra("command", 0);

        // Exit if no widgets
        if (allWidgetIds.length == 0) {
            return finishServiceIfDone();
        }

        if (command == DELETE_WIDGET) {
            for (int widgetID = allWidgetIds.length - 1; widgetID >= 0; --widgetID)
                for (int i = allWidgets.size() - 1; i >= 0; --i)
                    if (allWidgets.keyAt(i) == allWidgetIds[widgetID])
                        allWidgets.removeAt(i);
            return finishServiceIfDone();
        }

        NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public void onServiceReady(NetpowerctrlService mDiscoverService) {
                NetpowerctrlApplication.instance.unregisterServiceReadyObserver(this);

                for (int appWidgetId : allWidgetIds) {
                    SharedPrefs.WidgetOutlet outlet = SharedPrefs.LoadWidget(context, appWidgetId);
                    DeviceInfo di = null;
                    if (outlet != null) {
                        di = NetpowerctrlApplication.instance.findDevice(outlet.deviceMac);
                    }

                    if (outlet == null || di == null) {
                        setWidgetStateBroken(appWidgetId);
                        continue;
                    }

                    allWidgets.append(appWidgetId, new DeviceInfoOutletNumber(di, outlet.outletNumber));
                    onDeviceUpdated(di);
                }

            }
        });

        return finishServiceIfDone();
    }

    private void setWidgetStateBroken(int appWidgetId) {
        @SuppressWarnings("ConstantConditions")
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setImageViewResource(R.id.widget_image, R.drawable.widgetunknown);
        views.setTextViewText(R.id.widget_name, getString(R.string.error_widget_device_removed));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setWidgetState(int appWidgetId, SceneOutlet c, boolean reachable) {
        c.state = 2; // switch outlet
        OutletInfo oi = c.outletinfo;
        Scene og = new Scene();
        og.add(c);

        // This intent will be executed by a click on the widget
        Intent clickIntent = new Intent(context, ShortcutExecutionActivity.class);
        clickIntent.putExtra("commands", og.toString());
        //clickIntent.putExtra("widgetId", appWidgetId);
        clickIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        if (!reachable) {
            views.setImageViewUri(R.id.widget_image,
                    WidgetPreferenceFragment.getURI(this, appWidgetId, "widget_image_not_reachable"));
            views.setTextViewText(R.id.widget_name,
                    context.getString(R.string.widget_outlet_not_reachable, oi.getDescription()));
        } else {
            views.setImageViewUri(R.id.widget_image, WidgetPreferenceFragment.getURI(this, appWidgetId,
                    oi.State ? "widget_image_on" : "widget_image_off"));

            views.setTextViewText(R.id.widget_name, oi.getDescription());
        }
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static public void ForceUpdate(Context ctx, int widgetId) {
        Intent updateWidget = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null,
                ctx, DeviceWidgetProvider.class);
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
        if (di == null)
            return;

        for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
            int appWidgetId = widgetUpdateRequests.keyAt(i);
            DeviceInfoOutletNumber widget_di = widgetUpdateRequests.get(appWidgetId);
            if (widget_di.di.equalsFunctional(di)) {
                widgetUpdateRequests.removeAt(i);
                OutletInfo oi = di.findOutlet(widget_di.outletNumber);
                setWidgetState(appWidgetId, SceneOutlet.fromOutletInfo(oi, true), di.reachable);
                finishServiceIfDone();
                return;
            }
        }

        finishServiceIfDone();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        if (di == null)
            return;

        if (NetpowerctrlApplication.suspendWidgetUpdate > 0) {
            --NetpowerctrlApplication.suspendWidgetUpdate;
            return;
        }

        /**
         * If the service is kept running, we will receive further device updates
         * and update the widgets here.
         */
        ComponentName thisWidget = new ComponentName(context,
                DeviceWidgetProvider.class);
        //noinspection ConstantConditions
        appWidgetManager = AppWidgetManager.getInstance(context);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : allWidgetIds) {
            DeviceInfoOutletNumber widget_di = allWidgets.get(appWidgetId);
            if (widget_di == null) {
//                    ShowToast.FromOtherThread(this,
//                            getString(R.string.error_widget_update, appWidgetId ));
                continue;
            }

            if (widget_di.di.equalsFunctional(di)) {
                OutletInfo oi = widget_di.di.findOutlet(widget_di.outletNumber);
                if (!widget_di.isSameAsLastState(oi.State ? 1 : 0)) {
                    setWidgetState(appWidgetId, SceneOutlet.fromOutletInfo(oi, true), di.reachable);
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
        if (!SharedPrefs.PREF_keep_widget_service_running.equals(key))
            return;
        keep_service_online = SharedPrefs.getKeepWidgetServiceOn(this);
        finishServiceIfDone();
    }

    private int finishServiceIfDone() {
        if (widgetUpdateRequests.size() == 0 && !keep_service_online) {
            stopSelf();
        }
        return keep_service_online ? START_STICKY : START_NOT_STICKY;
    }
}
