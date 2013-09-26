package oly.netpowerctrl.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;

import oly.netpowerctrl.R;
import android.app.Activity;
import android.widget.Toast;

public class UDPSendToDevice {

	/**
	 * Convenience version of sendOutlet for the extra bundle of a shortcut intent.
	 * @param context
	 * @param data The bundle has to contain fields
	 * for "Device" (mac address string), "OutletNumber" (int) and "State" (bool)
	 * @return Return true if all fields have been found
	 */
	static public boolean sendOutlet(final Activity context, OutletCommandGroup og) {
		// Read configured devices, because we need the DebiceInfo objects that matches
		// our stored mac addresses
		ArrayList<DeviceInfo> alDevices = SharedPrefs.ReadConfiguredDevices(context);
		boolean r = SharedPrefs.getNextToggleState(context);
		for (OutletCommand c: og.commands) {
			// find DeviceInfo with matching mac address
			DeviceInfo di = null;
			for(DeviceInfo check: alDevices) {
				if (check.MacAddress.equals(c.device_mac)) {
					di = check;
					break;
				}
			}
			if (di!=null) {
				sendOutlet(context, di, c.outletNumber, (c.state==2)?r:(c.state==1?true:false));
			}
		}
		return true;
	}
	
	static public void sendOutlet(final Activity context, final DeviceInfo device, final int OutletNumber, final boolean sw_state) {
		// udp sending in own socket
		new Thread(new Runnable() {
			public void run() {
				try {
			        String messageStr = String.format(Locale.US,"%s%d%s%s", sw_state ? "Sw_on" : "Sw_off", OutletNumber, device.UserName, device.Password);
			        DatagramSocket s = new DatagramSocket();
					InetAddress host = InetAddress.getByName(device.HostName);
			        int msg_length=messageStr.length();
			        byte[] message = messageStr.getBytes();
			        DatagramPacket p = new DatagramPacket(message, msg_length, host, device.SendPort);
					s.send(p);
			        s.close();
				} catch (final IOException e) {
					context.runOnUiThread(new Runnable() {
					    public void run() {
					    	Toast.makeText(context, context.getResources().getString(R.string.error_sending_inquiry) +": "+ e.getMessage(), Toast.LENGTH_SHORT).show();
					    }
					});
				}
			}
		}).start();
	}

}
