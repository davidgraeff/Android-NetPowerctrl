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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.device_base.executables.ExecutableType;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceReady;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

/**
 * Widget Update Service
 */
public class WidgetUpdateService extends Service implements onDeviceObserverResult,
        onCollectionUpdated, onServiceReady, onDataLoaded {
    public static final int UPDATE_WIDGET = 0;
    public static final int DELETE_WIDGET = 1;
    public static final int CLICK_WIDGET = 2;
    private static final String TAG = "WidgetUpdateService";
    private final Map<Integer, WidgetExecutable> allWidgets = new TreeMap<>();
    private final Set<Integer> widgetsExecuting = new TreeSet<>();
    private final List<WidgetClick> widgetClicks = new ArrayList<>();
    private AppWidgetManager appWidgetManager;
    private int[] updateWidgetIDs = null;
    private boolean initDone = false;

    static public void ForceUpdate(Context ctx, int widgetId) {
        Intent updateWidget = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null,
                ctx, DeviceWidgetProvider.class);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
        updateWidget.putExtra(DeviceWidgetProvider.EXTRA_WIDGET_IDS, new int[]{widgetId});
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
    public void onCreate() {
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (ListenService.isServiceReady() && allWidgets.size() > 0)
            InAppNotifications.showException(this, null, "WidgetService: Unexpected request to close");

        /**
         * If the service is kept running but now should be finished (preferences changed,
         * system ends service) unregister from the listener service and from the shared preferences
         * changed signal.
         */
        AppData.getInstance().deviceCollection.unregisterObserver(this);
        AppData.getInstance().sceneCollection.unregisterObserver(this);
        ListenService.observersServiceReady.unregister(this);
        ListenService.stopUseService();
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
        // Empty intent: System recreated the service after low mem situation
        if (intent != null) {
            // Extract widget ids from intent
            updateWidgetIDs = intent.getIntArrayExtra(DeviceWidgetProvider.EXTRA_WIDGET_IDS);
            int command = intent.getIntExtra(DeviceWidgetProvider.EXTRA_WIDGET_COMMAND, UPDATE_WIDGET);

            if (command == DELETE_WIDGET) {
                for (int updateWidgetIndex = updateWidgetIDs.length - 1; updateWidgetIndex >= 0; --updateWidgetIndex)
                    allWidgets.remove(updateWidgetIDs[updateWidgetIndex]);
                return finishServiceIfDone();
            } else if (command == CLICK_WIDGET) {
                widgetClicks.add(new WidgetClick(intent.getStringExtra(EditSceneActivity.RESULT_ACTION_UUID),
                        intent.getIntExtra(EditSceneActivity.RESULT_ACTION_COMMAND, -1)));
            }
        }

        if (updateWidgetIDs == null) {
            // Only update widgets belonging to DeviceWidgetProvider
            ComponentName thisWidget = new ComponentName(getApplicationContext(),
                    DeviceWidgetProvider.class);
            updateWidgetIDs = appWidgetManager.getAppWidgetIds(thisWidget);
            allWidgets.clear();
        }

        // Exit if no widgets
        if (updateWidgetIDs == null || updateWidgetIDs.length == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!initDone) {
            initDone = true;
            AppData.useAppData();
            ListenService.useService(getApplicationContext(), false, false);
            ListenService.observersServiceReady.register(this);
        } else
            preCheckUpdate();

        return START_STICKY;
    }

    private void preCheckUpdate() {
        if (!ListenService.isServiceReady())
            return;

        if (!AppData.isDataLoaded()) {
            AppData.observersOnDataLoaded.register(this);
            return;
        }

        // Filter: Only those widgets ids with linked preferences are valid
        if (updateWidgetIDs != null) {
            AppData appData = AppData.getInstance();
            for (int appWidgetId : updateWidgetIDs) {
                String executable_uid = SharedPrefs.getInstance().LoadWidget(appWidgetId);
                Executable executable = null;
                Device device = null;
                if (executable_uid != null) {
                    executable = appData.findDevicePort(executable_uid);
                }
                if (executable != null)
                    device = ((DevicePort) executable).device;
                else {
                    executable = appData.sceneCollection.findScene(executable_uid);
                    DevicePort master = null;
                    if (executable != null)
                        master = appData.findDevicePort(((Scene) executable).getMasterUUid());
                    if (master != null)
                        device = master.device;
                }


                if (executable == null) {
                    Log.e(TAG, "Loading widget failed: " + String.valueOf(appWidgetId));
                    setWidgetStateBroken(appWidgetId);
                    continue;
                }

                allWidgets.put(appWidgetId, new WidgetExecutable(executable, device, appWidgetId));
            }

            // Exit if no widgets
            if (allWidgets.size() == 0) {
                stopSelf();
                return;
            }

            updateWidgetIDs = null;
        }

        if (widgetClicks.size() > 0) {
            for (WidgetClick widgetClick : widgetClicks)
                executeSingleAction(widgetClick.uuid, widgetClick.command);
            widgetClicks.clear();
        } else
            updateDevices();
    }

    private void setWidgetStateBroken(int appWidgetId) {
        @SuppressWarnings("ConstantConditions")
        RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
        views.setImageViewBitmap(R.id.widget_image,
                LoadStoreIconData.loadBitmap(this, LoadStoreIconData.uuidFromDefaultWidget(),
                        LoadStoreIconData.IconType.WidgetIcon, LoadStoreIconData.IconState.StateUnknown));
        views.setTextViewText(R.id.widget_name, getString(R.string.error_widget_device_removed));
        views.setViewVisibility(R.id.widget_status, View.GONE);
        views.setViewVisibility(R.id.widget_inProgress, View.GONE);

        widgetsExecuting.remove(appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private WidgetExecutable getWidgetEntryByExecutable(Executable executable) {
        for (Map.Entry<Integer, WidgetExecutable> entry : allWidgets.entrySet()) {
            WidgetExecutable widgetExecutable = entry.getValue();
            if (widgetExecutable.executable == executable) {
                return widgetExecutable;
            }
        }
        return null;
    }

    private void executeSingleAction(String executable_uid, final int command) {
        Executable executable = AppData.getInstance().findDevicePort(executable_uid);
        if (executable == null) {
            executable = AppData.getInstance().sceneCollection.findScene(executable_uid);
        }
        if (executable == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            return;
        }

        final WidgetExecutable widgetEntry = getWidgetEntryByExecutable(executable);

        if (widgetEntry == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            return;
        }

        if (widgetEntry.device != null) {
            setWidgetState(widgetEntry.widgetID, executable, true);

            if (!executable.isReachable()) {
                ListenService.getService().findDevices(true, null);
                return;
            }
        }

        final long currentTime = System.currentTimeMillis();
        AppData.getInstance().executeToggle(executable, new onExecutionFinished() {
            @Override
            public void onExecutionFinished(int commands) {
                // Fail safe: If no response from the device, we set the widget to broken state
                if (widgetEntry.device != null)
                    App.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (widgetEntry.device.getUpdatedTime() < currentTime)
                                setWidgetState(widgetEntry.widgetID, widgetEntry.executable, false);
                        }
                    }, 1200);
            }
        });
    }

    private void setWidgetState(final int appWidgetId, final Executable executable, final boolean inProgress) {
        // Delay new data to show inProgress state for at least 500ms.
        if (widgetsExecuting.contains(appWidgetId) && !inProgress) {
            widgetsExecuting.remove(appWidgetId);
            App.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setWidgetState(appWidgetId, executable, false);
                }
            }, 500);
            return;
        }

        Intent clickIntent = new Intent(this, WidgetUpdateService.class);
        clickIntent.putExtra(EditSceneActivity.RESULT_ACTION_UUID, executable.getUid());
        clickIntent.putExtra(EditSceneActivity.RESULT_ACTION_COMMAND, DevicePort.TOGGLE);
        clickIntent.putExtra(DeviceWidgetProvider.EXTRA_WIDGET_COMMAND, CLICK_WIDGET);
        PendingIntent pendingIntent = PendingIntent.getService(this, (int) System.currentTimeMillis(), clickIntent, 0);

        // Load preferences
        boolean widget_show_title;
        boolean widget_show_status;
        boolean widget_use_default;

        {
            String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            SharedPreferences widgetPreferences;
            widgetPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
            widget_use_default = widgetPreferences.getBoolean("widget_use_default", getResources().getBoolean(R.bool.widget_use_default));
            if (widget_use_default) {
                prefName = SharedPrefs.PREF_WIDGET_BASENAME;
                widgetPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
            }
            widget_show_title = widgetPreferences.getBoolean("widget_show_title", getResources().getBoolean(R.bool.widget_show_title));
            widget_show_status = widgetPreferences.getBoolean("widget_show_status", getResources().getBoolean(R.bool.widget_show_status));
        }

        // Do not show a status text line ("on"/"off") for a simple trigger
        if (executable.getType() == ExecutableType.TypeStateless)
            widget_show_status = false;

        // Manipulate view
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget);

        views.setViewVisibility(R.id.widget_name, widget_show_title ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_status, widget_show_status ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_inProgress, inProgress ? View.VISIBLE : View.GONE);
        //views.setFloat(R.id.widget_inProgress,"setAlpha",1.0f);

        LoadStoreIconData.IconState iconState;
        int string_res;
        if (!executable.isReachable()) { // unreachable
            string_res = R.string.widget_outlet_not_reachable;
            iconState = LoadStoreIconData.IconState.StateUnknown;
        } else if (executable.getCurrentValue() > 0) { // On
            string_res = R.string.widget_on;
            iconState = LoadStoreIconData.IconState.StateOn;
        } else {
            string_res = R.string.widget_off;
            iconState = LoadStoreIconData.IconState.StateOff;
        }

        if (inProgress) {
            widgetsExecuting.add(appWidgetId);
            string_res = R.string.widget_inProgress;
        }

        views.setTextViewText(R.id.widget_name, executable.getTitle(this));
        views.setTextViewText(R.id.widget_status, this.getString(string_res));

        // If the device is not reachable there is no sense in assigning a click event pointing to
        // the ExecutionActivity. We do that nevertheless here to let the ExecutionActivity
        // figure out if the device is still not reachable
        if (!inProgress) {
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_name, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_status, pendingIntent);
        }
        Bitmap bitmap;
        if (widget_use_default)
            bitmap = LoadStoreIconData.loadBitmap(this, LoadStoreIconData.uuidFromDefaultWidget(),
                    LoadStoreIconData.IconType.WidgetIcon, iconState);
        else
            bitmap = LoadStoreIconData.loadBitmap(this, LoadStoreIconData.uuidFromWidgetID(appWidgetId),
                    LoadStoreIconData.IconType.WidgetIcon, iconState);
        views.setImageViewBitmap(R.id.widget_image, bitmap);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onObserverDeviceUpdated(Device device) {
        updated(null, device, ObserverUpdateActions.UpdateAction, -1);
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        for (Device device : timeout_devices) {
            updated(null, device, ObserverUpdateActions.UpdateAction, -1);
        }
    }

    @Override
    public boolean onServiceReady(ListenService service) {
        AppData.getInstance().deviceCollection.registerObserver(this);
        AppData.getInstance().sceneCollection.registerObserver(this);
        preCheckUpdate();
        return true;
    }

    @Override
    public void onServiceFinished() {
        if (ListenService.isServiceUsed())
            InAppNotifications.showException(this, null, "WidgetService: ListenService unexpectedly closed");
        stopSelf();
    }

    private int finishServiceIfDone() {
        if (allWidgets.size() == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    /**
     * Update all widgets now. This is necessary only once as soon as
     * the listener service is ready and all app data has loaded.
     */
    private void updateDevices() {
        List<Device> devicesToUpdate = new ArrayList<>();
        for (Map.Entry<Integer, WidgetExecutable> entry : allWidgets.entrySet()) {
            WidgetExecutable widgetExecutable = entry.getValue();
            if (widgetExecutable.executable.isReachable())
                setWidgetState(entry.getKey(), widgetExecutable.executable, false);
            else if (widgetExecutable.device != null) {
                setWidgetState(entry.getKey(), widgetExecutable.executable, true);
                devicesToUpdate.add(widgetExecutable.device);
            }
        }

        if (devicesToUpdate.size() > 0)
            new DeviceQuery(this, this, devicesToUpdate.iterator());
        else
            finishServiceIfDone();
    }

    @Override
    public boolean updated(Object collection, Object item, ObserverUpdateActions action, int position) {
        //Log.w("widget", di != null ? di.DeviceName : "empty di");
        if (item == null)
            return true;

        for (Map.Entry<Integer, WidgetExecutable> entry : allWidgets.entrySet()) {
            WidgetExecutable widgetExecutable = entry.getValue();
            if (collection instanceof DeviceCollection) {
                if (widgetExecutable.device != null && widgetExecutable.device.equalsByUniqueID((Device) item)) {
                    setWidgetState(entry.getKey(), widgetExecutable.executable, false);
                }
            } else // scenes
                if (widgetExecutable.executable == item) {
                    setWidgetState(entry.getKey(), widgetExecutable.executable, false);
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

    private static class WidgetClick {
        int command;
        String uuid;

        private WidgetClick(String uuid, int command) {
            this.command = command;
            this.uuid = uuid;
        }
    }

    private class WidgetExecutable {
        Executable executable;
        Device device;
        int widgetID;

        private WidgetExecutable(Executable executable, @Nullable Device device, int widgetID) {
            this.executable = executable;
            this.device = device;
            this.widgetID = widgetID;
        }
    }
}
