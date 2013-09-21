package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControlActivity extends Activity implements OnClickListener {

	final Activity _this = this;
	DeviceInfo device;
	List<CompoundButton> buttons;
	ImageView imgReceive;
	boolean firstPacketReceived = false;
	
	// for remembering what the device sent back
	Map<Integer,Boolean> lastreceivedState;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		lastreceivedState = new HashMap<Integer,Boolean>();

		Intent it = new Intent(this, NetpowerctrlService.class);
		startService(it);
		
		buttons = new ArrayList<CompoundButton>();
		
		device = null;
		it = getIntent();
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
						   getResources().getString(R.string.error_unknown_device),
						   Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		setContentView(R.layout.device_control);
		((TextView)findViewById(R.id.tvDeviceName)).setText(device.DeviceName);
		imgReceive = (ImageView)findViewById(R.id.imgReceive);
		
		int top_margin = 10;
		if (Build.VERSION.SDK_INT >= 14) {
			// uses the "switch" widget
			top_margin = 30;
		}

		if (Build.VERSION.SDK_INT < 11) {
			// receive image is not handled by ObjectAnimator, but simply on/off
			imgReceive.setVisibility(View.INVISIBLE);
		}
		
		buttons.clear();
		LinearLayout ll = (LinearLayout)findViewById(R.id.llDeviceControl);
		for (OutletInfo oi: device.Outlets) {
			CompoundButton cb = null;
			if (Build.VERSION.SDK_INT >= 14) 
				cb = new Switch(this);
				else cb = new CheckBox(this);	
			cb.setEnabled(false); // will only be enabled when the first answer arrives
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
    	
    	// disable all buttons, will be re-enabled when the first answer arrives
    	for (View v: buttons)
    		v.setEnabled(false);
    	
    	IntentFilter itf= new IntentFilter(DiscoveryThread.BROADCAST_DEVICE_DISCOVERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onDeviceDiscovered, itf);
        firstPacketReceived = false;
        onFirstPacketCheck.sendMessageDelayed(onFirstPacketCheck.obtainMessage(), 2000); // after 2 seconds, check if no packet was received and display the respective dialog
        DeviceQuery.sendQuery(this, device.HostName, device.SendPort);
    }
    
    @Override
    protected void onPause() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(onDeviceDiscovered);
    	super.onPause();
	}

	@Override
	public void onBackPressed() {
		// always go back to the main activity, or we may have to
		// traverse endless DeviceConfig Activities from widgets
		Intent it = new Intent(this, NetpowerctrlActivity.class);
		startActivity(it);
		super.onBackPressed();
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
		menu.add(0, R.id.menu_refresh, 0, R.string.menu_refresh).setIcon(R.drawable.ic_menu_refresh);
		if (Build.VERSION.SDK_INT >= 11) {
			menu.findItem(R.id.menu_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		return true;
	}

	public void onClick(View v) {
		int outletNumber = (Integer)v.getTag();
		if (outletNumber >= 0) {
			boolean new_state = ((CompoundButton)v).isChecked();
			AfterSentHandler ash = new AfterSentHandler(outletNumber, new_state);
			ash.sendMessageDelayed(ash.obtainMessage(), 500); // check after 500 ms
			sendOutlet(outletNumber, new_state);
		} else {
			Toast.makeText(this,
					   getResources().getString(R.string.error_outlet_number),
					   Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh: {
	        DeviceQuery.sendQuery(this, device.HostName, device.SendPort);
			return true;
		}
		
		}
		return false;
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
	    @SuppressLint("NewApi")
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
				// remember this state
				lastreceivedState.clear();
				for (OutletInfo oi: device_info.Outlets) 
					lastreceivedState.put(oi.OutletNumber, oi.State); 
				
				// update outlet states
				for (CompoundButton button: buttons) {
					if (lastreceivedState.containsKey((Integer)button.getTag()))
						button.setChecked(lastreceivedState.get((Integer)button.getTag()));
				}
				if (Build.VERSION.SDK_INT >= 11) {
					ObjectAnimator anim = ObjectAnimator.ofFloat(imgReceive, "Alpha", 0, 1, 0);
					anim.setDuration(400);
					anim.setInterpolator(new AccelerateDecelerateInterpolator());
					anim.start();
				} else {
					imgReceive.setVisibility(View.VISIBLE);
					new Handler().postDelayed(new Runnable() { 
				         public void run() { 
				        	 imgReceive.setVisibility(View.INVISIBLE);
				         } 
				    }, 400);
				}
				
				// re-enable all buttons
				for (View v: buttons)
					v.setEnabled(true);
				firstPacketReceived = true; // some way, every packet is the first packet ;-)
				
			}
	    }
	};

	private Handler onFirstPacketCheck= new Handler() {
		public void handleMessage(Message m) {
			if (! firstPacketReceived) {
		    	Toast.makeText(_this, getResources().getString(R.string.error_getting_first_packet), Toast.LENGTH_LONG).show();
				for (View v: buttons)
					v.setEnabled(true);
			}
		}
	};
	
	private class AfterSentHandler extends Handler {
		int outletNumber; // remember for which outlet we were started
		boolean state;    // the state we want the outlet to be in
		int retries;
		
		public AfterSentHandler(int outletNr, boolean expected_state) {
			outletNumber = outletNr;
			state = expected_state;
			retries = 0;
		}
		
	    public void handleMessage(Message m) {
			if (retries > 3) {
				//give up
		    	Toast.makeText(_this, getResources().getString(R.string.error_setting_outlet), Toast.LENGTH_LONG).show();
				return;
			}
			
			if (lastreceivedState.containsKey(outletNumber)) {
				if (lastreceivedState.get(outletNumber) != state) {
					retries++;
					sendMessageDelayed(obtainMessage(), 500); // check again after 500 ms
					sendOutlet(outletNumber, state);
					// show the current state
					for (CompoundButton button: buttons) {
						if ((Integer)button.getTag() == outletNumber) {
							button.setChecked(!state);
						}
					}
				}
				
			} else {
				// nothing received yet, try again
				retries++;
				sendMessageDelayed(obtainMessage(), 500); // check again after 500 ms
				sendOutlet(outletNumber, state);
			}
	    }

	}
}
