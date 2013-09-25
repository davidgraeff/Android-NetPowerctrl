package oly.netpowerctrl.service;

import java.util.ArrayList;

import oly.netpowerctrl.DeviceInfo;
import oly.netpowerctrl.OutledListAdapter;
import oly.netpowerctrl.UDPSendToDevice;
import oly.netpowerctrl.utils.SharedPrefs;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShortcutExecutionActivity extends Activity
{
	ArrayList<DeviceInfo> alDevices;
	OutledListAdapter adpOutlets;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ArrayList<DeviceInfo> alDevices = SharedPrefs.ReadConfiguredDevices(this);

		Intent it = getIntent();
		if (it == null) {
			finish();
			return;
		}
		
		Bundle extra = it.getExtras();
		if (extra == null) {
			finish();
			return;
		}
		
		String mac = null;
		int OutletNumber = -1;
		DeviceInfo di = null;
		Object o = extra.get("Device");
		if (o == null) {
			finish();
			return;
		}
		mac = (String)o;
		o = extra.get("OutletNumber");
		if (o == null) {
			finish();
			return;
		}
		OutletNumber = (Integer)o;
		for(DeviceInfo check: alDevices) {
			if (check.MacAddress.equals(mac)) {
				di = check;
				break;
			}
		}
		
		UDPSendToDevice.sendOutlet(this, di, OutletNumber, true);
		setResult(RESULT_OK,null);
		finish();
	}
}