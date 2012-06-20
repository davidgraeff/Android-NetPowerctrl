package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControl extends Activity implements OnClickListener {

	DeviceInfo device;
	List<CompoundButton> buttons;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		buttons = new ArrayList<CompoundButton>();
		
		device = null;
		Intent it = getIntent();
		if (it != null) {
			Bundle extra = it.getExtras();
			if (extra != null) {
				Object o = extra.get("device");
				if (o != null) {
					device = (DeviceInfo) o; 
				}
			}
		}

		if (device == null) {
			Toast.makeText(this,
						   getResources().getString(R.string.error_creating_device_control),
						   Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		setContentView(R.layout.device_control);
		((TextView)findViewById(R.id.tvDeviceName)).setText(device.DeviceName);
		
		int top_margin = 10;
		if (Build.VERSION.SDK_INT >= 14) {
			// uses the "switch" widget
			top_margin = 30;
		}
		
		buttons.clear();
		LinearLayout ll = (LinearLayout)findViewById(R.id.llDeviceControl);
		for (OutletInfo oi: device.Outlets) {
			CompoundButton cb = null;
			if (Build.VERSION.SDK_INT >= 14) 
				cb = new Switch(this);
				else cb = new CheckBox(this);	
			cb.setChecked(oi.State);
			cb.setTag(oi.OutletNumber);
			cb.setText(oi.Description);
			cb.setOnClickListener(this);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
			lp.setMargins(0, top_margin, 0, 0);
			ll.addView(cb, lp);
			buttons.add(cb);
		}
	}
	
	
    @Override
    protected void onResume() {
    	super.onResume();
    	IntentFilter itf= new IntentFilter(DiscoveryThread.BROADCAST_DEVICE_DISCOVERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onDeviceDiscovered, itf);
        DeviceQuery.sendQuery(this, device.HostName, device.SendPort);
    }
    
    @Override
    protected void onPause() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(onDeviceDiscovered);
    	super.onPause();
	}

	public void onClick(View v) {
		int outletNumber = (Integer)v.getTag();
		if (outletNumber >= 0) {
			sendOutlet(outletNumber, ((CompoundButton)v).isChecked());
		} else {
			Toast.makeText(this,
					   getResources().getString(R.string.error_outlet_number),
					   Toast.LENGTH_LONG).show();
		}
	}
	
	public void sendOutlet(final int number, final boolean state) {
		final Activity self = this;
		new Thread(new Runnable() {
			public void run() {
				try {
			        String messageStr = String.format("%s%d%s%s", state ? "Sw_on" : "Sw_off", number, device.UserName, device.Password);
			        DatagramSocket s = new DatagramSocket();
					InetAddress host = InetAddress.getByName(device.HostName);
			        int msg_length=messageStr.length();
			        byte[] message = messageStr.getBytes();
			        DatagramPacket p = new DatagramPacket(message, msg_length, host, device.SendPort);
					s.send(p);
			        s.close();
				} catch (final IOException e) {
					runOnUiThread(new Runnable() {
					    public void run() {
					    	Toast.makeText(self, getResources().getString(R.string.error_sending_inquiry) +": "+ e.getMessage(), Toast.LENGTH_LONG).show();
					    }
					});
				}
			}
		}).start();
	}
	
	private BroadcastReceiver onDeviceDiscovered= new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	DeviceInfo device_info = null;
			Bundle extra = intent.getExtras();
			if (extra != null) {
				Object o = extra.get("device_info");
				if (o != null) {
					device_info = (DeviceInfo) o; 
				}
			}
			if (device_info == null)
				return;

			// our device?
			if (device.MacAddress.equals(device_info.MacAddress)) {
				// update outlet states
				for (CompoundButton button: buttons) {
					for (OutletInfo oi: device_info.Outlets) {
						if (oi.OutletNumber == (Integer)button.getTag()) {
							button.setChecked(oi.State);
						}
					}
				}
			}
	    }
	};
	
}
