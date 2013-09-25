package oly.netpowerctrl.service;

import java.util.ArrayList;

import oly.netpowerctrl.DeviceInfo;
import oly.netpowerctrl.OutledListAdapter;
import oly.netpowerctrl.OutletInfo;
import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.SharedPrefs;
import android.app.ListActivity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class ShortcutCreatorActivity extends ListActivity implements OnItemClickListener
{
	ArrayList<DeviceInfo> alDevices;
	OutledListAdapter adpOutlets;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED,null);
		getListView().setOnItemClickListener(this);
		
    	alDevices = SharedPrefs.ReadConfiguredDevices(this);
  		adpOutlets = new OutledListAdapter(this, alDevices);
  		setListAdapter(adpOutlets);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		OutletInfo oi = (OutletInfo)adpOutlets.getItem(position);
		final Intent shortcutIntent=new Intent(this, oly.netpowerctrl.service.ShortcutExecutionActivity.class);
		shortcutIntent.putExtra("OutletNumber", oi.OutletNumber);
		shortcutIntent.putExtra("Device", oi.device.MacAddress);
		final ShortcutIconResource iconResource=Intent.ShortcutIconResource.fromContext(this,R.drawable.netpowerctrl);
		
		// Return result
		final Intent intent=new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,getResources().getString(R.string.app_name) + " " + oi.Description);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,iconResource);
		setResult(RESULT_OK,intent);
		
		// Show info message
		Toast.makeText(this,"shortcut: "+oi.Description,Toast.LENGTH_SHORT).show();
		finish();
	}
}