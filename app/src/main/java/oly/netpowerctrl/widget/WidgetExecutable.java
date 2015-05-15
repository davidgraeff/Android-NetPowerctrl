package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.IconState;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.devices.DevicesObserver;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.ExecutionActivity;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Created by david on 15.04.15.
 */
public class WidgetExecutable extends AbstractWidget implements onCollectionUpdated<ExecutableCollection, Executable>, DevicesObserver.onDevicesObserverFinished {
    private static final String TAG = "WidgetEntryExecutable";
    int cached_last_state = -1000;
    private Executable executable = null;
    private Bitmap image_broken;
    private Context context;
    private AppWidgetManager appWidgetManager;
    private boolean isInProgress = false;

    public WidgetExecutable(WidgetUpdateService widgetUpdateService, int widgetID, DataService dataService) {
        super(widgetUpdateService, widgetID, ProviderExecutable.getTypeString());
        init(widgetUpdateService);
        loadExecutable(dataService);
    }

    private void loadExecutable(DataService dataService) {
        String executableID = SharedPrefs.getInstance().loadWidget(widgetID, ProviderExecutable.getTypeString());

        if (executableID != null) {
            executable = dataService.executables.findByUID(executableID);
        }
        Credentials credentials = null;

        if (executable != null) {
            if (executable instanceof Scene) {
                Executable master = dataService.executables.findByUID(((Scene) executable).getMasterExecutableUid());
                if (master != null)
                    credentials = dataService.credentials.findByUID(master.deviceUID);
            } else
                credentials = dataService.credentials.findByUID(executable.deviceUID);
        }

        if (executable == null) {
            Log.e(TAG, "Loading widget failed: " + String.valueOf(widgetID) + " " + executableID);
            setWidgetStateBroken(widgetID);
        } else {
            if (executable.reachableState() == ReachabilityStates.Reachable)
                setWidgetState(widgetID, executable, false);
            else if (credentials != null) {
                setWidgetState(widgetID, executable, true);
                dataService.refreshExistingDevice(credentials, this);
            }
        }

        DataService.useService(new WeakReference<Object>(this));
        dataService.executables.registerObserver(this);
    }

    @Override
    void forceUpdate(DataService dataService) {
        if (executable == null)
            loadExecutable(dataService);
        else
            setWidgetState(widgetID, executable, false);
    }

    @Override
    public void destroy() {
        if (DataService.isServiceReady()) {
            widgetUpdateService.service.executables.unregisterObserver(this);
        }

        DataService.stopUseService(this);
        super.destroy();
    }

    @Override
    void click(int position) {
        setWidgetState(widgetID, executable, true);
    }

    @Override
    public void onObserverJobFinished(DevicesObserver devicesObserver) {
        if (devicesObserver.timedOutDevices().size() > 0)
            setWidgetStateBroken(widgetID);
    }

    @Override
    public boolean updated(@NonNull ExecutableCollection collection, Executable item, @NonNull ObserverUpdateActions action) {
        if (item == null) return true;
        //Log.w("widget", di != null ? di.DeviceName : "empty di");

        if (executable.getCurrentValue() == cached_last_state || executable.equals(item)) {
            setWidgetState(widgetID, executable, false);
        }

        return true;
    }

    void init(Context context) {
        this.context = context;
        appWidgetManager = AppWidgetManager.getInstance(context);
        try {
            image_broken = LoadStoreIconData.loadDefaultBitmap(context, IconState.StateUnknown,
                    SharedPrefs.getDefaultFallbackIconSet(context));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    void setWidgetStateBroken(int appWidgetId) {
        @SuppressWarnings("ConstantConditions")
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_executable);
        views.setImageViewBitmap(R.id.widget_image, image_broken);
        views.setTextViewText(R.id.widget_name, context.getString(R.string.error_widget_needs_configuration));
        views.setViewVisibility(R.id.widget_status, View.GONE);
        views.setViewVisibility(R.id.widget_inProgress, View.GONE);

        Intent clickIntent = new Intent(context, ConfigExecutableActivity.class);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_name, pendingIntent);

        isInProgress = false;

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    void setWidgetState(final int appWidgetId, final Executable executable, final boolean inProgress) {
        if (isInProgress && !inProgress) {
            isInProgress = false;
            App.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setWidgetState(appWidgetId, executable, false);
                }
            }, 500);
            return;
        }

        // Load preferences
        SharedPreferences widgetPreferences = context.getSharedPreferences(ProviderExecutable.getTypeString(), Context.MODE_PRIVATE);
        boolean widget_show_title = widgetPreferences.getBoolean("widget_show_title", context.getResources().getBoolean(R.bool.widget_show_title));
        boolean widget_show_status = widgetPreferences.getBoolean("widget_show_status", context.getResources().getBoolean(R.bool.widget_show_status));

        // Do not show a status text line ("on"/"off") for a simple trigger
        if (executable.getType() == ExecutableType.TypeStateless)
            widget_show_status = false;

        // Manipulate view
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_executable);

        views.setViewVisibility(R.id.widget_name, widget_show_title ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_status, widget_show_status ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_inProgress, inProgress ? View.VISIBLE : View.GONE);
        //views.setFloat(R.id.widget_inProgress,"setAlpha",1.0f);

        IconState iconState;
        int string_res;
        if (executable.reachableState() == ReachabilityStates.NotReachable) { // unreachable
            string_res = R.string.widget_outlet_not_reachable;
            iconState = IconState.StateUnknown;
        } else if (executable.getCurrentValue() > 0) { // On
            string_res = R.string.widget_on;
            iconState = IconState.StateOn;
        } else {
            string_res = R.string.widget_off;
            iconState = IconState.StateOff;
        }

        if (inProgress) {
            isInProgress = true;
            string_res = R.string.widget_inProgress;
        }

        views.setTextViewText(R.id.widget_name, executable.getTitle());
        views.setTextViewText(R.id.widget_status, context.getString(string_res));

        // If the device is not reachable there is no sense in assigning a click event pointing to
        // the ExecutionActivity. We do that nevertheless to let the ExecutionActivity
        // figure out if the device is still not reachable
        if (!inProgress) {
            Intent clickIntent = new Intent(context, WidgetUpdateService.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            clickIntent.putExtra(WidgetUpdateService.EXTRA_WIDGET_TYPE, ProviderExecutable.getTypeString());
            clickIntent.putExtra(WidgetUpdateService.EXTRA_WIDGET_COMMAND, WidgetUpdateService.CLICK_WIDGET);
            clickIntent.putExtra(ExecutionActivity.EXECUTE_ACTION_UUID, executable.getUid());
            clickIntent.putExtra(ExecutionActivity.EXECUTE_ACTION_COMMAND, Executable.TOGGLE);
            PendingIntent pendingIntent = PendingIntent.getService(context, (int) System.currentTimeMillis(), clickIntent, 0);

            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_name, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_status, pendingIntent);
        }
        Bitmap bitmap = LoadStoreIconData.loadBitmap(context, executable, iconState, null);
        views.setImageViewBitmap(R.id.widget_image, bitmap);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
