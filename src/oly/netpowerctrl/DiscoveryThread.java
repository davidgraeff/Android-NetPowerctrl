package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.widget.Toast;

public class DiscoveryThread extends Thread {
	
	Activity activity;
	DeviceFoundEvent found_event;
	
	public DiscoveryThread(Activity act, DeviceFoundEvent dfe) {
		activity = act;
		found_event = dfe;
	}
	
	public void run() {

		sendQuery();
		
		while (getState() != Thread.State.TERMINATED) {
			try {
				int recv_port = activity.getResources().getInteger(R.integer.default_recv_port); //TODO: make configurable
				byte[] message = new byte[1500];
		        DatagramPacket p = new DatagramPacket(message, message.length);
		        DatagramSocket s = new DatagramSocket(recv_port);
		        s.receive(p);
		        s.close();
		        parsePacket(new String(message, 0, p.getLength()), recv_port);
			} catch (final IOException e) {
				activity.runOnUiThread(new Runnable() {
				    public void run() {
				    	Toast.makeText(null, e.getMessage(), Toast.LENGTH_LONG).show();
				    }
				});
			}
		}
	}

	
	public void sendQuery() {
		try {
	        String messageStr="wer da?\r\n";
	        int server_port = activity.getResources().getInteger(R.integer.default_send_port); //TODO: make configurable
	        DatagramSocket s;
			s = new DatagramSocket();
			//TODO s.setBroadcast(true);
			InetAddress local;
			local = InetAddress.getByName("angrenostpwr.nittka.com"); // TODO
	        int msg_length=messageStr.length();
	        byte[] message = messageStr.getBytes();
	        DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
			s.send(p);
	        s.close();
		} catch (final IOException e) {
			activity.runOnUiThread(new Runnable() {
			    public void run() {
			    	Toast.makeText(null, e.getMessage(), Toast.LENGTH_LONG).show();
			    }
			});
		}
	}
	
	public void parsePacket(final String message, int recevied_port) {
		
		String msg[] = message.split(":");
		if (msg.length < 3)
			return;
		
		final DeviceInfo di = new DeviceInfo();
		di.DeviceName = msg[0];
		di.HostName = msg[2];
		di.RecvPort = recevied_port;
		di.SendPort = activity.getResources().getInteger(R.integer.default_send_port); // that's where we were sending after all
		
		for (int i=6; i<msg.length; i++) {
			String outlet[] = msg[i].split(",");
			if (outlet.length < 1)
				continue;
			OutletInfo oi = new OutletInfo();
			oi.Description = outlet[0];
			if (outlet.length > 1)
				oi.State = outlet[1] != "0";
			di.Outlets.add(oi);
		}
		
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				found_event.onDeviceFound(di);
			}
		});
		
	}
}
