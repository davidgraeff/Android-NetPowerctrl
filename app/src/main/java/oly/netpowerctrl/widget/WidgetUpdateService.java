package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.Shortcuts;
import oly.netpowerctrl.utils.Icons;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements DeviceObserverResult, DeviceUpdate, ServiceReady {
    private static final String LOG = "WidgetUpdateService";
    public static final int UPDATE_WIDGET = 0;
    public static final int DELETE_WIDGET = 1;
    private AppWidgetManager appWidgetManager;
    private Context context;

    private final List<Integer> widgetUpdateRequests = new ArrayList<Integer>();
    private final SparseArray<DevicePort> allWidgets = new SparseArray<DevicePort>();

    @Override
    public void onDestroy() {
        /**
         * If the service is kept running but now should be finished (preferences changed,
         * system ends service) unregister from the listener service and from the shared preferences
         * changed signal.
         */
        NetpowerctrlApplication.getDataController().deviceCollection.unregisterConfiguredDeviceChangeObserver(this);
        NetpowerctrlApplication.instance.unregisterServiceReadyObserver(this);
        NetpowerctrlApplication.instance.stopUseListener();
    }

    @Override
    public void onCreate() {
        //noinspection ConstantConditions
        context = getApplicationContext();
        assert context != null;
        appWidgetManager = AppWidgetManager.getInstance(context);
        NetpowerctrlApplication.instance.useListener();
        NetpowerctrlApplication.getDataController().deviceCollection.registerConfiguredDeviceChangeObserver(this);
        NetpowerctrlApplication.instance.registerServiceReadyObserver(this);
        super.onCreate();
    }

    private int[] getAllWidgetIDs() {
        // Only update widgets belonging to DeviceWidgetProvider
        ComponentName thisWidget = new ComponentName(context,
                DeviceWidgetProvider.class);
        return appWidgetManager.getAppWidgetIds(thisWidget);
    }

    private void updateDevices() {
        if (!NetpowerctrlApplication.instance.isServiceReady())
            return;

        List<DeviceInfo> devicesToUpdate = new ArrayList<DeviceInfo>();
        int[] allWidgetIds = getAllWidgetIDs();
        for (int appWidgetId : allWidgetIds) {
            String port_uuid = SharedPrefs.LoadWidget(appWidgetId);
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(
                    port_uuid == null ? null : UUID.fromString(port_uuid));
            if (port == null) {
                setWidgetStateBroken(appWidgetId);
                continue;
            }

            allWidgets.append(appWidgetId, port);
            if (port.device.getUpdatedTime() > 0)
                onDeviceUpdated(port.device, false);
            else {
                devicesToUpdate.add(port.device);
                widgetUpdateRequests.add(appWidgetId);
            }
        }

        if (devicesToUpdate.size() > 0)
            new DeviceQuery(this, devicesToUpdate);

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

        updateDevices();

        return START_STICKY;
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
        Scene.SceneItem item = new Scene.SceneItem(oi.uuid, DevicePort.TOGGLE);

        // This intent will be executed by a click on the widget
        Intent clickIntent = Shortcuts.createShortcutExecutionIntent(context, item, false, true);
        clickIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        // Load preferences
        String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        boolean widget_show_text = getSharedPreferences(prefName, MODE_PRIVATE).getBoolean("widget_show_text", true);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Do not show a status text line ("on"/"off") for a simple trigger
        if (oi.getType() == DevicePort.DevicePortType.TypeButton)
            widget_show_text = false;

        views.setViewVisibility(R.id.widget_name, widget_show_text ? View.VISIBLE : View.GONE);

        if (!oi.device.isReachable()) {
            views.setImageViewBitmap(R.id.widget_image,
                    Icons.loadIcon(this, Icons.uuidFromWidgetID(appWidgetId),
                            Icons.IconType.WidgetIcon, Icons.IconState.StateUnknown,
                            Icons.getResIdForState(Icons.IconState.StateUnknown))
            );
            views.setTextViewText(R.id.widget_name, oi.getDescription());
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_outlet_not_reachable));
            // Status Text is always visible even for simple triggers
            views.setViewVisibility(R.id.widget_status, View.VISIBLE);
            // If the device is not reachable there is no sense in assigning a click event pointing to
            // the ExecutionActivity. We do that nevertheless here to let the ExecutionActivity
            // figure out if the device is still not reachable
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        } else if (oi.current_value > 0) { // On
            views.setImageViewBitmap(R.id.widget_image,
                    Icons.loadIcon(this, Icons.uuidFromWidgetID(appWidgetId),
                            Icons.IconType.WidgetIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn))
            );
            views.setTextViewText(R.id.widget_name, oi.getDescription());
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_on));
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        } else { // Off
            views.setImageViewBitmap(R.id.widget_image,
                    Icons.loadIcon(this, Icons.uuidFromWidgetID(appWidgetId),
                            Icons.IconType.WidgetIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff))
            );

            views.setTextViewText(R.id.widget_name, oi.getDescription());
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_off));
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
        }

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
    public void onDeviceError(DeviceInfo di) {

    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        if (di == null)
            return;

        for (int i = widgetUpdateRequests.size() - 1; i >= 0; --i) {
            int appWidgetId = widgetUpdateRequests.get(i);
            DevicePort devicePort = allWidgets.get(appWidgetId);
            if (devicePort.device.equalsByUniqueID(di)) {
                widgetUpdateRequests.remove(i);
                setWidgetState(appWidgetId, devicePort);
                finishServiceIfDone();
                return;
            }
        }

        finishServiceIfDone();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        onDeviceUpdated(di, false);
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        //Log.w("widget", di != null ? di.DeviceName : "empty di");
        if (di == null)
            return;

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

            if (devicePort.device.equalsByUniqueID(di)) {
                setWidgetState(appWidgetId, devicePort);
            }
        }

        finishServiceIfDone();
    }

    @Override
    public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
        for (DeviceInfo di : timeout_devices)
            onDeviceUpdated(di, false);
    }

    @Override
    public boolean onServiceReady() {
        if (allWidgets.size() == 0)
            updateDevices();
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
}
