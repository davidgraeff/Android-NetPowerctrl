package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceReady;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.scenes.SceneItem;
import oly.netpowerctrl.utils.AndroidShortcuts;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements DeviceObserverResult, onCollectionUpdated<DeviceCollection, Device>, onServiceReady, onDataLoaded {
    public static final int UPDATE_WIDGET = 0;
    public static final int DELETE_WIDGET = 1;
    private static final String TAG = "WidgetUpdateService";
    //    private final List<Integer> widgetUpdateRequests = new ArrayList<>();
    private final SparseArray<DevicePort> allWidgets = new SparseArray<>();
    private AppWidgetManager appWidgetManager;
    private Context context;

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
    public void onDestroy() {
        /**
         * If the service is kept running but now should be finished (preferences changed,
         * system ends service) unregister from the listener service and from the shared preferences
         * changed signal.
         */
        AppData.getInstance().deviceCollection.unregisterObserver(this);
        ListenService.observersServiceReady.unregister(this);
        ListenService.stopUseService();
    }

    @Override
    public void onCreate() {
        //noinspection ConstantConditions
        context = getApplicationContext();
        assert context != null;
        appWidgetManager = AppWidgetManager.getInstance(context);
        AppData.useAppData();
        ListenService.useService(getApplicationContext(), false, false);
        ListenService.observersServiceReady.register(this);
        super.onCreate();
    }

    private int[] getAllWidgetIDs() {
        // Only update widgets belonging to DeviceWidgetProvider
        ComponentName thisWidget = new ComponentName(context,
                DeviceWidgetProvider.class);
        return appWidgetManager.getAppWidgetIds(thisWidget);
    }

    /**
     * Update all widgets now. This is necessary only once as soon as
     * the listener service is ready and all app data has loaded.
     */
    private void updateDevices() {
        List<Device> devicesToUpdate = new ArrayList<>();
        int[] allWidgetIds = getAllWidgetIDs();
        for (int appWidgetId : allWidgetIds) {
            String port_uuid = SharedPrefs.getInstance().LoadWidget(appWidgetId);
            if (port_uuid == null) {
                Log.e(TAG, "Loading widget failed: " + String.valueOf(appWidgetId));
                setWidgetStateBroken(appWidgetId);
                continue;
            }
            DevicePort port = AppData.getInstance().findDevicePort(
                    UUID.fromString(port_uuid));
            if (port == null) {
                setWidgetStateBroken(appWidgetId);
                continue;
            }

            // Log.e(TAG, "Load widget: "+String.valueOf(appWidgetId));

            allWidgets.append(appWidgetId, port);
            if (port.device.getUpdatedTime() > 0)
                updated(null, port.device, ObserverUpdateActions.UpdateAction);
            else {
                devicesToUpdate.add(port.device);
//                widgetUpdateRequests.add(appWidgetId);
            }
        }

        if (devicesToUpdate.size() > 0)
            new DeviceQuery(this, this, devicesToUpdate.iterator());

        finishServiceIfDone();
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
        int[] allWidgetIds;
        int command = UPDATE_WIDGET;

        // Empty intent: System recreated the service after low mem situation
        if (intent == null) {
            allWidgetIds = getAllWidgetIDs();
        } else {
            // Extract widget ids from intent
            allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            command = intent.getIntExtra("name", UPDATE_WIDGET);
        }

        // Exit if no widgets
        assert allWidgetIds != null;
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

        preCheckUpdate();

        return START_STICKY;
    }

    private void preCheckUpdate() {
        Log.w(TAG, "preCheckUpdate");
        if (ListenService.isServiceReady()) {
            Log.w(TAG, "service ready");
            if (AppData.observersOnDataLoaded.dataLoaded) {
                Log.w(TAG, "data ready");
                updateDevices();
            } else { // data not loaded
                AppData.observersOnDataLoaded.register(this);
            }
        }
    }

    private void setWidgetStateBroken(int appWidgetId) {
        @SuppressWarnings("ConstantConditions")
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setImageViewResource(R.id.widget_image, R.drawable.stateunknown);
        views.setTextViewText(R.id.widget_name, getString(R.string.error_widget_device_removed));
        views.setTextViewText(R.id.widget_status, "");

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setWidgetState(int appWidgetId, DevicePort oi) {
        SceneItem item = new SceneItem(oi.uuid, DevicePort.TOGGLE);

        // This intent will be executed by a click on the widget
        Intent clickIntent = AndroidShortcuts.createShortcutExecutionIntent(context, item, false, true);
        clickIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        // Load preferences
        boolean widget_show_title;
        boolean widget_show_status;
        boolean widget_use_default;

        {
            String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            SharedPreferences widgetPreferences;
            widgetPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
            widget_use_default = widgetPreferences.getBoolean("widget_use_default", true);
            if (widget_use_default) {
                prefName = SharedPrefs.PREF_WIDGET_BASENAME;
                widgetPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
            }
            widget_show_title = widgetPreferences.getBoolean("widget_show_title", true);
            widget_show_status = widgetPreferences.getBoolean("widget_show_status", true);
        }

        // Do not show a status text line ("on"/"off") for a simple trigger
        if (oi.getType() == DevicePort.DevicePortType.TypeButton)
            widget_show_status = false;

        // Manipulate view
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        views.setViewVisibility(R.id.widget_name, widget_show_title ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_status, widget_show_status ? View.VISIBLE : View.GONE);

        LoadStoreIconData.IconState iconState;
        int string_res;
        if (oi.device.getFirstReachableConnection() == null) { // unreachable
            // Status Text is always visible even for simple triggers
            views.setViewVisibility(R.id.widget_status, View.VISIBLE);
            string_res = R.string.widget_outlet_not_reachable;
            iconState = LoadStoreIconData.IconState.StateUnknown;
        } else if (oi.current_value > 0) { // On
            string_res = R.string.widget_on;
            iconState = LoadStoreIconData.IconState.StateOn;
        } else {
            string_res = R.string.widget_off;
            iconState = LoadStoreIconData.IconState.StateOff;
        }

        views.setTextViewText(R.id.widget_name, oi.getDescription());
        views.setTextViewText(R.id.widget_status, context.getString(string_res));
        // If the device is not reachable there is no sense in assigning a click event pointing to
        // the ExecutionActivity. We do that nevertheless here to let the ExecutionActivity
        // figure out if the device is still not reachable
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
        Bitmap bitmap;
        if (widget_use_default)
            bitmap = LoadStoreIconData.loadIcon(this, LoadStoreIconData.uuidFromDefaultWidget(),
                    LoadStoreIconData.IconType.WidgetIcon, iconState,
                    LoadStoreIconData.getResIdForState(iconState));
        else
            bitmap = LoadStoreIconData.loadIcon(this, LoadStoreIconData.uuidFromWidgetID(appWidgetId),
                    LoadStoreIconData.IconType.WidgetIcon, iconState,
                    LoadStoreIconData.getResIdForState(iconState));
        views.setImageViewBitmap(R.id.widget_image, bitmap);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onObserverDeviceUpdated(Device device) {
        updated(null, device, ObserverUpdateActions.UpdateAction);
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        for (Device device : timeout_devices) {
            updated(null, device, ObserverUpdateActions.UpdateAction);
        }
    }

    @Override
    public boolean onServiceReady(ListenService service) {
        AppData.getInstance().deviceCollection.registerObserver(this);
        preCheckUpdate();
        return true;
    }

    @Override
    public void onServiceFinished() {
        ACRA.getErrorReporter().handleException(
                new Exception("WidgetService unexpected service close"), true);
        stopSelf();
    }

    private int finishServiceIfDone() {
        if (getAllWidgetIDs().length == 0) {
            Log.w("WidgetUpdateService", "finish");
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action) {
        //Log.w("widget", di != null ? di.DeviceName : "empty di");
        if (device == null)
            return true;

        /**
         * If the service is kept running, we will receive further device updates
         * and update the widgets here.
         */
        ComponentName thisWidget = new ComponentName(context,
                DeviceWidgetProvider.class);
        //noinspection ConstantConditions
        appWidgetManager = AppWidgetManager.getInstance(context);
        assert appWidgetManager != null;
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : allWidgetIds) {
            DevicePort devicePort = allWidgets.get(appWidgetId);
            if (devicePort == null) {
                setWidgetStateBroken(appWidgetId);
                continue;
            }

            if (devicePort.device.equalsByUniqueID(device)) {
                setWidgetState(appWidgetId, devicePort);
            }
        }

        finishServiceIfDone();
        return true;
    }

    @Override
    public boolean onDataLoaded() {
        preCheckUpdate();
        // unregister now
        return false;
    }
}
