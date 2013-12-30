package oly.netpowerctrl.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;

class WidgetConfig extends Activity {
    private Context ctx;
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private List<DeviceInfo> devices;
    private List<OutletInfo> outlets;
    private String selectedDeviceMac;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        devices = SharedPrefs.ReadConfiguredDevices(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        CharSequence[] items = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++)
            items[i] = devices.get(i).DeviceName;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_widget_device);
        builder.setItems(items, selectedDeviceListener);
        AlertDialog alert = builder.create();
        alert.setOnCancelListener(cancelListener);
        alert.show();
    }

    private DialogInterface.OnCancelListener cancelListener = new OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    };

    private DialogInterface.OnClickListener selectedDeviceListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {

            // Get outlets of device
            outlets = devices.get(item).Outlets;
            selectedDeviceMac = devices.get(item).MacAddress;

            CharSequence[] items = new String[outlets.size()];
            for (int i = 0; i < outlets.size(); i++)
                items[i] = outlets.get(i).Description;

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle(R.string.choose_widget_outlet);
            builder.setItems(items, selectedOutletListener);
            AlertDialog alert = builder.create();
            alert.setOnCancelListener(cancelListener);
            alert.show();
        }
    };

    private DialogInterface.OnClickListener selectedOutletListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {

            SharedPrefs.SaveWidget(ctx, widgetId, selectedDeviceMac, item);

            WidgetUpdate.Update(ctx, widgetId);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, resultValue);

            finish();
        }
    };

}
