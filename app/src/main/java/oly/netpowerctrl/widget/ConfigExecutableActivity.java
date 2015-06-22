package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.adapter.InputConfiguredExecutables;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.SelectFromExecutableListFragment;

public class ConfigExecutableActivity extends Activity implements SelectFromExecutableListFragment.onItemClicked {
    private int widgetId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        FragmentUtils.makeActivityDialog(this);
        setContentView(R.layout.activity_content_only);

        Bundle extras = getIntent().getExtras();
        widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            throw new RuntimeException();

        SelectFromExecutableListFragment s = new SelectFromExecutableListFragment(this,
                new InputConfiguredExecutables());
        getFragmentManager().beginTransaction().replace(R.id.content_frame, s).commit();
    }

    @Override
    public void onExecutableSelected(String uid, int position) {
        SharedPrefs.getInstance().saveWidget(widgetId, uid, ProviderExecutable.getTypeString());

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(Activity.RESULT_OK, resultValue);
        finish();

        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                WidgetUpdateService.ForceUpdate(ConfigExecutableActivity.this, widgetId, ProviderExecutable.class);
            }
        });
    }

    @Override
    public void onGroupSelected(String uid, int position) {

    }
}
