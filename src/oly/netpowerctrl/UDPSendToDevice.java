package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;

import android.app.Activity;
import android.widget.Toast;

public class UDPSendToDevice {

	static public void sendOutlet(final Activity context, final DeviceInfo device, final int OutletNumber, final boolean state) {
		new Thread(new Runnable() {
			public void run() {
				try {
			        String messageStr = String.format(Locale.US,"%s%d%s%s", state ? "Sw_on" : "Sw_off", OutletNumber, device.UserName, device.Password);
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
					    	Toast.makeText(context, context.getResources().getString(R.string.error_sending_inquiry) +": "+ e.getMessage(), Toast.LENGTH_LONG).show();
					    }
					});
				}
			}
		}).start();
	}

}
