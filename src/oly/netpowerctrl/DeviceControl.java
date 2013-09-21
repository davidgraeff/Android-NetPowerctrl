package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

public class DeviceControl implements OnClickListener {
	DeviceInfo device;
	List<CompoundButton> buttons;
	Activity act;
	
	// for remembering what the device sent back
	Map<Integer,Boolean> lastreceivedState;


	@SuppressLint("NewApi")
	public DeviceControl(Activity act, DeviceInfo device) {
		this.act = act;
		this.device = device;
		lastreceivedState = new HashMap<Integer,Boolean>();
		buttons = new ArrayList<CompoundButton>();
//		Intent it = new Intent(act, NetpowerctrlService.class);
//		act.startService(it);

		int top_margin = 10;
		if (Build.VERSION.SDK_INT >= 14) {
			// uses the "switch" widget
			top_margin = 30;
		}

		buttons.clear();
		LinearLayout ll = (LinearLayout)act.findViewById(R.id.llAllOutlets);
		for (OutletInfo oi: device.Outlets) {
			CompoundButton cb = null;
			if (Build.VERSION.SDK_INT >= 14) 
				cb = new Switch(act);
			else
				cb = new CheckBox(act);	
			cb.setEnabled(true);
			cb.setChecked(oi.State);
			cb.setTag(oi.OutletNumber);
			cb.setText(oi.Description);
			cb.setOnClickListener(this);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			lp.setMargins(0, top_margin, 0, 0);
			ll.addView(cb, lp);
			buttons.add(cb);
		}
	}
	
	public void clear() {
		buttons.clear();
	}

	public void onClick(View v) {
		int outletNumber = (Integer)v.getTag();
		if (outletNumber >= 0) {
			boolean new_state = ((CompoundButton)v).isChecked();
			AfterSentHandler ash = new AfterSentHandler(outletNumber, new_state);
			ash.sendMessageDelayed(ash.obtainMessage(), 500); // check after 500 ms
			sendOutlet(outletNumber, new_state);
		} else {
			Toast.makeText(act,
					   act.getResources().getString(R.string.error_outlet_number),
					   Toast.LENGTH_LONG).show();
		}
	}

	public void sendOutlet(final int number, final boolean state) {
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
					act.runOnUiThread(new Runnable() {
					    public void run() {
					    	Toast.makeText(act, act.getResources().getString(R.string.error_sending_inquiry) +": "+ e.getMessage(), Toast.LENGTH_LONG).show();
					    }
					});
				}
			}
		}).start();
	}
	
	public void onReceive(DeviceInfo device_info) {
		// remember this state
		lastreceivedState.clear();
		for (OutletInfo oi: device_info.Outlets) 
			lastreceivedState.put(oi.OutletNumber, oi.State); 
		
		// update outlet states
		for (CompoundButton button: buttons) {
			if (lastreceivedState.containsKey((Integer)button.getTag())) {
				button.setChecked(lastreceivedState.get((Integer)button.getTag()));
				GreenFlasher.flashBgColor(button);
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
		    	Toast.makeText(act, act.getResources().getString(R.string.error_setting_outlet), Toast.LENGTH_LONG).show();
				return;
			}
			
			if (lastreceivedState.containsKey(outletNumber)) {
				if (lastreceivedState.get(outletNumber) != state) {
					if (retries==0) {
						// show the current state
						for (CompoundButton button: buttons) {
							if ((Integer)button.getTag() == outletNumber) {
								button.setChecked(!state);
							}
						}
					}
					retries++;
					Log.w("DeviceControl","Sending again, no response "+Integer.valueOf(outletNumber).toString());
					sendMessageDelayed(obtainMessage(), 500); // check again after 500 ms
					sendOutlet(outletNumber, state);
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
