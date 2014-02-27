package oly.netpowerctrl.widget;

import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsBaseAdapter;
import oly.netpowerctrl.main.OutletsSceneEditFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.OutletsManipulator;

public class WidgetConfig extends Activity implements OutletsManipulator {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    DevicePortsBaseAdapter adapter;

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
        setContentView(R.layout.activity_main);

        Fragment f = new OutletsSceneEditFragment(this,
                OutletsSceneEditFragment.MANIPULATOR_TAG_AVAILABLE,
                this);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, f).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    private AdapterView.OnItemClickListener selectedOutletListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            SharedPrefs.SaveWidget(widgetId, adapter.getItem(position).uuid.toString());

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, resultValue);
            finish();

            WidgetUpdateService.ForceUpdate(WidgetConfig.this, widgetId);
        }
    };

    @Override
    public void setManipulatorObjects(int tag, DynamicGridView view, DevicePortsBaseAdapter adapter) {
        view.setOnItemClickListener(selectedOutletListener);
        adapter.update(NetpowerctrlApplication.getDataController().configuredDevices);
        this.adapter = adapter;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish();
    }
}
