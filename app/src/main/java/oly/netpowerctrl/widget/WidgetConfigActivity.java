package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeStateChanged;
import oly.netpowerctrl.devices.DevicePortsBaseAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.EditSceneFragment;
import oly.netpowerctrl.scenes.EditSceneFragmentReady;

public class WidgetConfigActivity extends Activity implements EditSceneFragmentReady {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private DevicePortsBaseAdapter adapter;

    @Override
    protected void onDestroy() {
        NetpowerctrlApplication.instance.stopUseListener();
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        NetpowerctrlApplication.instance.useListener();
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
        f.setData(this,
                EditSceneFragment.TYPE_AVAILABLE,
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

    @Override
    public void sceneEditFragmentReady(EditSceneFragment fragment) {
        fragment.getListView().setOnItemClickListener(selectedOutletListener);
        this.adapter = fragment.getAdapter();
        NetpowerctrlApplication.getDataController().registerStateChanged(new RuntimeStateChanged() {
            @Override
            public boolean onDataLoaded() {
                return true; // let this callback stay registered
            }

            @Override
            public boolean onDataQueryFinished() {
                adapter.update(NetpowerctrlApplication.getDataController().deviceCollection.devices);
                return false; // we are not interested in another call
            }
        }, true);
    }

    @Override
    public void entryDismiss(EditSceneFragment fragment, int position) {

    }
}
