package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.widget.Toast;

public class DeviceQuery {

	public static void sendQuery(final Activity activity, final String hostname, final int port) {
		new Thread(new Runnable() {
			public void run() {
				try {
			        String messageStr="wer da?\r\n";
			        DatagramSocket s = new DatagramSocket();
					s.setBroadcast(true);
					InetAddress host = InetAddress.getByName(hostname);
			        int msg_length=messageStr.length();
			        byte[] message = messageStr.getBytes();
			        DatagramPacket p = new DatagramPacket(message, msg_length, host, port);
					s.send(p);
			        s.close();
				} catch (final IOException e) {
					activity.runOnUiThread(new Runnable() {
					    public void run() {
					    	Toast.makeText(activity, activity.getResources().getString(R.string.error_sending_inquiry) +": "+ e.getMessage(), Toast.LENGTH_LONG).show();
					    }
					});
				}
			}
		}).start();
	}
	
	public static void sendBroadcastQuery(final Activity activity) {
		// make a unique list of the default port and all configured devices
		HashSet<Integer> ports = new HashSet<Integer>();
		ports.add(activity.getResources().getInteger(R.integer.default_send_port)); //TODO: make configurable);
		ArrayList<Integer> uniquePorts = new ArrayList<Integer>(ports);
		
		for (int port: uniquePorts) 
			sendQuery(activity, "255.255.255.255", port);
	}
	

}
