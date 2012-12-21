package oly.netpowerctrl;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;

public class WidgetConfig extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int tmp = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (extras != null) {
		    tmp = extras.getInt(
		            AppWidgetManager.EXTRA_APPWIDGET_ID, 
		            AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		final int widgetId = tmp;

		final List<DeviceInfo> devices = SharedPrefs.ReadConfiguredDevices(this);
		final CharSequence[] items = new String[devices.size()];
		for (int i=0; i<devices.size(); i++)
			items[i] = devices.get(i).DeviceName;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.choose_widget_device);
		final Context ctx = this;
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	SharedPrefs.SaveDevice(ctx, SharedPrefs.PREF_WIDGET_BASENAME+String.valueOf(widgetId), devices.get(item));

		    	Intent updateWidget = new Intent();   
		    	updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		    	updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
		    	sendBroadcast(updateWidget);
		    	
		    	Intent resultValue = new Intent();
		    	resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		    	setResult(RESULT_OK, resultValue);

		        finish();
		    }
		});
		AlertDialog alert = builder.create();
		alert.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		alert.show();
	}

}
