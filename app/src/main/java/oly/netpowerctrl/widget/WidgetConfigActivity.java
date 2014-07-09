package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsBaseAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.EditSceneFragment;
import oly.netpowerctrl.scenes.EditSceneFragmentReady;

public class WidgetConfigActivity extends Activity implements EditSceneFragmentReady {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private final AdapterView.OnItemClickListener selectedOutletListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            SharedPrefs.SaveWidget(widgetId, adapter.getItem(position).uuid.toString());

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
        NetpowerctrlService.stopUseListener();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        NetpowerctrlService.useListener();
        NetpowerctrlService.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                service.findDevices(null);
                return false;
            }

            @Override
            public void onServiceFinished() {

            }
        });

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        // Set theme, call super onCreate and set content view
        if (SharedPrefs.isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }
        setContentView(R.layout.activity_main_one_pane);
        findViewById(R.id.left_drawer_list).setVisibility(View.GONE);
        getActionBar().setHomeButtonEnabled(false);

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
        //TODO adapter
        DevicePortSourceConfigured s = new DevicePortSourceConfigured();
        this.adapter = new DevicePortsListAdapter(this, false, s);
        fragment.setAdapter(this.adapter);
    }
}
