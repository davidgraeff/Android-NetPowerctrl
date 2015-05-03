package oly.netpowerctrl.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.main.ExecutionActivity;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

;

/**
 * Widget Update Service for all types of widgets.
 */
public class WidgetUpdateService extends RemoteViewsService implements onServiceReady {
    public static final int UPDATE_WIDGET = 0;
    public static final int DELETE_WIDGET = 1;
    public static final int CLICK_WIDGET = 2;

    public static final String EXTRA_WIDGET_COMMAND = "command";
    public static final String EXTRA_WIDGET_TYPE = "type";
    public static final String EXTRA_WIDGET_CLICK_POSITION = "position";
    private static final String TAG = "WidgetUpdateService";

    AppWidgetManager appWidgetManager;
    DataService service;
    Map<String, AbstractWidget> widgets = new TreeMap<>();
    private String stopReason = "";

    static public void ForceUpdate(Context ctx, int widgetId, Class<? extends AppWidgetProvider> widgetProviderClass) {
        Intent updateWidget = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, ctx, widgetProviderClass);
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
        ctx.sendBroadcast(updateWidget);
    }

    @SuppressWarnings("unused")
    static public void ForceUpdateAll(Context ctx, Class<? extends AppWidgetProvider> widgetProviderClass) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        ComponentName thisAppWidget = new ComponentName(ctx.getPackageName(), widgetProviderClass.getName());

        @SuppressWarnings("ConstantConditions")
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        assert appWidgetIds != null;
        for (int id : appWidgetIds) {
            ForceUpdate(ctx, id, widgetProviderClass);
        }
    }

    @Override
    public void onCreate() {
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        DataService.useService(new WeakReference<Object>(this));
        DataService.observersServiceReady.register(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (DataService.isServiceReady() && !widgets.isEmpty() && !stopReason.isEmpty())
            InAppNotifications.silentException(new Exception("WidgetService: Unexpected request to close: " + stopReason), null);

        Logging.getInstance().logWidgets("Service close");

        for (AbstractWidget e : widgets.values()) {
            e.destroy();
        }

        widgets.clear();

        DataService.observersServiceReady.unregister(this);
        DataService.stopUseService(this);
    }


    /**
     * This method will be called by every startService call.
     * We update a widget here, if the android system requested a widget update.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!DataService.isServiceReady())
            return START_STICKY;

        // Empty intent: System recreated the service after low mem situation
        if (intent != null) {
            // Extract widget ids from intent
            int command = intent.getIntExtra(EXTRA_WIDGET_COMMAND, UPDATE_WIDGET);
            String widgetType = intent.getStringExtra(EXTRA_WIDGET_TYPE);

            if (command == DELETE_WIDGET) {
                int[] updateWidgetIDs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                for (int updateWidgetIndex = updateWidgetIDs.length - 1; updateWidgetIndex >= 0; --updateWidgetIndex) {
                    String wKey = makeWKey(updateWidgetIDs[updateWidgetIndex], widgetType);
                    AbstractWidget entry = widgets.get(wKey);
                    if (entry != null) {
                        entry.remove();
                        widgets.remove(wKey);
                    }
                }
                return finishServiceIfDone();

            } else if (command == CLICK_WIDGET) {
                int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                AbstractWidget entry = widgets.get(makeWKey(widgetID, widgetType));

                String action_uid = intent.getStringExtra(ExecutionActivity.EXECUTE_ACTION_UUID);
                int action_command = intent.getIntExtra(ExecutionActivity.EXECUTE_ACTION_COMMAND, -1);

                if (click(action_uid, action_command)) {
                    int list_position = intent.getIntExtra(EXTRA_WIDGET_CLICK_POSITION, -1);
                    entry.click(list_position);
                }

            } else if (command == UPDATE_WIDGET) {
                int[] updateWidgetIDs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                for (int updateWidgetIndex = updateWidgetIDs.length - 1; updateWidgetIndex >= 0; --updateWidgetIndex) {
                    String wKey = makeWKey(updateWidgetIDs[updateWidgetIndex], widgetType);
                    AbstractWidget entry = widgets.get(wKey);
                    if (entry == null)
                        loadWidgetEntry(updateWidgetIDs[updateWidgetIndex], widgetType, service);
                    if (entry != null) entry.forceUpdate(service);
                }

            } else {
                throw new RuntimeException();
            }
        }

        return START_STICKY;
    }

    private void loadWidgetEntry(int widgetID, String widgetType, DataService dataService) {
        String wKey = makeWKey(widgetID, widgetType);
        AbstractWidget e = widgets.get(wKey);
        if (e != null) return;

        if (widgetType.equals(ProviderExecutable.getTypeString())) {
            e = new WidgetExecutable(this, widgetID, dataService);
            widgets.put(wKey, e);
        } else if (widgetType.equals(ProviderGroup.getTypeString())) {
            e = new WidgetGroupList(this, widgetID, dataService);
            widgets.put(wKey, e);
        } else
            throw new RuntimeException();
    }

    public String makeWKey(int widgetID, String widgetType) {
        return String.valueOf(widgetID) + widgetType;
    }

    boolean click(String action_uid, int action_command) {
        DataService dataService = service;

        Executable executable = dataService.executables.findByUID(action_uid);
        if (executable != null) {
            executable.execute(dataService, action_command, null);
            return true;
        }

        Toast.makeText(service, service.getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
        return false;
    }

    // This is for the group-type of widgets only.
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID)
            throw new RuntimeException("Invalid widget ID");
        String wKey = makeWKey(widgetID, ProviderGroup.getTypeString());

        WidgetGroupList e = (WidgetGroupList) widgets.get(wKey);
        if (e == null) {
            e = new WidgetGroupList(this, widgetID, null);
            widgets.put(wKey, e);
        }

        return e;
    }


    @Override
    public boolean onServiceReady(DataService service) {
        this.service = service;
        Logging.getInstance().logWidgets("Service start");

        int[] l1 = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), ProviderExecutable.class));
        for (int widgetID : l1)
            loadWidgetEntry(widgetID, ProviderExecutable.getTypeString(), service);
        int[] l2 = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), ProviderGroup.class));
        for (int widgetID : l2) loadWidgetEntry(widgetID, ProviderGroup.getTypeString(), service);

        // Exit if no widgets
        if (l1.length == 0 && l2.length == 0) {
            stopReason = "onStart:no widgets";
            stopSelf();
            return false;
        }

        return true;
    }

    @Override
    public void onServiceFinished(DataService service) {
        stopReason = "ListenService finished!";
        stopSelf();
    }

    private int finishServiceIfDone() {
        if (widgets.isEmpty()) {
            stopReason = "finishServiceIfDone: no widgets";
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }
}
