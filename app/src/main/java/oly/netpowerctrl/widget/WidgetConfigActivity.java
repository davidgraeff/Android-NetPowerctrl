package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.executables.AdapterFragment;
import oly.netpowerctrl.executables.ExecutablesBaseAdapter;
import oly.netpowerctrl.executables.ExecutablesListAdapter;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ExecutablesBaseAdapter adapter;

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
        findViewById(R.id.toolbar_actionbar).setVisibility(View.GONE);

        ExecutablesSourceDevicePorts s = new ExecutablesSourceDevicePorts(null);
        s.setAutomaticUpdate(true);
        this.adapter = new ExecutablesListAdapter(false, s, LoadStoreIconData.iconLoadingThread, true);

        AdapterFragment<ExecutablesBaseAdapter> f = new AdapterFragment<>();
        f.setAdapter(this.adapter);

        f.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
                    return false;
                String uid = adapter.getItem(position).getExecutableUid();
                if (uid == null)
                    return false;

                SharedPrefs.getInstance().SaveWidget(widgetId, uid);

                App.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Intent resultValue = new Intent();
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                        setResult(RESULT_OK, resultValue);
                        finish();
                        WidgetUpdateService.ForceUpdate(WidgetConfigActivity.this, widgetId);
                    }
                });
                return true;
            }
        }, null));

        getFragmentManager().beginTransaction().replace(R.id.content_frame, f).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }
}
