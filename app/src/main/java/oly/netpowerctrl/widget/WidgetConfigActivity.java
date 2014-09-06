package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsBaseAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.scenes.EditSceneFragment;
import oly.netpowerctrl.scenes.onEditSceneFragmentReady;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;

public class WidgetConfigActivity extends Activity implements onEditSceneFragmentReady, ActivityWithIconCache {
    private final IconDeferredLoadingThread mIconCache = new IconDeferredLoadingThread();
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private final AdapterView.OnItemClickListener selectedOutletListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            SharedPrefs.getInstance().SaveWidget(widgetId, adapter.getDevicePort(position).uuid.toString());

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, resultValue);
            finish();

            WidgetUpdateService.ForceUpdate(WidgetConfigActivity.this, widgetId);
        }
    };
    private DevicePortsBaseAdapter adapter;

    @Override
    protected void onPause() {
        ListenService.stopUseService();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        AppData.useAppData();
        ListenService.useService(getApplicationContext(), true, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }
        setContentView(R.layout.activity_main_one_pane);
        findViewById(R.id.left_drawer_list).setVisibility(View.GONE);
        getActionBar().setHomeButtonEnabled(false);

        mIconCache.start();

        EditSceneFragment f = new EditSceneFragment();
        f.setReadyObserver(this);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, f).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    @Override
    public void sceneEditFragmentReady(EditSceneFragment fragment) {
        fragment.getListView().setOnItemClickListener(selectedOutletListener);
        DevicePortSourceConfigured s = new DevicePortSourceConfigured();
        s.setAutomaticUpdate(true);
        this.adapter = new DevicePortsListAdapter(this, false, s, mIconCache, true);
        fragment.setAdapter(this.adapter);
    }

    @Override
    public IconDeferredLoadingThread getIconCache() {
        return mIconCache;
    }
}
