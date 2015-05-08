package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.groups.GroupAdapter;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;

public class ConfigGroupActivity extends Activity implements RecyclerItemClickListener.OnItemClickListener, EmptyListener {
    private int widgetId;
    private GroupAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

//         Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkThemeDialog);
        } else {
            setTheme(R.style.Theme_CustomLightThemeDialog);
        }
        FragmentUtils.makeActivityDialog(this);

        setContentView(R.layout.fragment_with_list);

        Bundle extras = getIntent().getExtras();
        widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            throw new RuntimeException();

        adapter = new GroupAdapter(false);
        adapter.setEmptyListener(this);
        RecyclerViewWithAdapter<?> recyclerViewWithAdapter =
                new RecyclerViewWithAdapter<>(this, findViewById(R.id.layout_with_list), adapter, 0);
        recyclerViewWithAdapter.setOnItemClickListener(new RecyclerItemClickListener(this, ConfigGroupActivity.this, null));
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        String uid = adapter.getGroup(position).uid;
        SharedPrefs.getInstance().saveWidget(widgetId, uid, ProviderGroup.getTypeString());

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(Activity.RESULT_OK, resultValue);
        finish();

        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                WidgetUpdateService.ForceUpdate(ConfigGroupActivity.this, widgetId, ProviderGroup.class);
            }
        });
        return true;
    }

    @Override
    public void onEmptyListener(boolean empty) {
        if (!empty) return;
        findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.empty_text)).setText(R.string.widget_group_no_group);
    }
}