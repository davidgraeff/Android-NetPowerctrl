package oly.netpowerctrl.service;

import java.util.ArrayList;

import oly.netpowerctrl.listadapter.OutledListAdapter;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.outletconfig.OutletConfig;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.OutletCommandGroup;
import oly.netpowerctrl.utils.UDPSendToDevice;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ShortcutExecutionActivity extends Activity
{
	ArrayList<DeviceInfo> alDevices;
	OutledListAdapter adpOutlets;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Intent it = getIntent();
		if (it == null) {
			finish();
			return;
		}
		
		Bundle extra = it.getExtras();
		OutletCommandGroup g = OutletCommandGroup.fromString(extra.getString("commands"), this);
		if (g == null) {
			Toast.makeText(this, "Shortcut not valid!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (UDPSendToDevice.sendOutlet(this, g))
			setResult(RESULT_OK,null);
		
		if (extra.getBoolean("show_mainwindow")) {
			Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
			mainIt.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(mainIt);
		}
		finish();
	}
}