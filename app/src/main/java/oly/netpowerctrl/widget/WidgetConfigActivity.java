package oly.netpowerctrl.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.SharedPrefs;

public class WidgetConfigActivity extends Activity {

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
        setContentView(R.layout.activity_content_only);

        Bundle extras = getIntent().getExtras();
        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new WidgetConfigFragment(widgetId)).commit();

    }
}
