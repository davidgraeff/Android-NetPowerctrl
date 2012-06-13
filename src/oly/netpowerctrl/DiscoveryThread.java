package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.app.Activity;
import android.widget.Toast;

public class DiscoveryThread extends Thread {
	
	Activity activity;
	DeviceFoundEvent found_event;
	boolean keep_running;
	DatagramSocket socket;
	
	public DiscoveryThread(Activity act, DeviceFoundEvent dfe) {
		activity = act;
		found_event = dfe;
		socket = null;
	}
	
	public void run() {

		keep_running = true;
		while (keep_running) {
			try {
				int recv_port = activity.getResources().getInteger(R.integer.default_recv_port); //TODO: make configurable
				byte[] message = new byte[1500];
		        DatagramPacket p = new DatagramPacket(message, message.length);
		        socket = new DatagramSocket(recv_port);
		        socket.receive(p);
		        if (! socket.isClosed()) {
		        	socket.close();
		        	parsePacket(new String(message, 0, p.getLength()), recv_port);
		        }
			} catch (final IOException e) {
				if (keep_running) { // no message if we were interrupt()ed
					activity.runOnUiThread(new Runnable() {
					    public void run() {
					    	Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
					    }
					});
				}
			}
		}
	}

	@Override
	public void interrupt() {
	    super.interrupt();
	    keep_running = false;
	    if (socket != null)
	    	this.socket.close();
	}

	
	public void parsePacket(final String message, int recevied_port) {
		
		String msg[] = message.split(":");
		if (msg.length < 3)
			return;
		
		if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
			// error packet received
			String desc;
			if (msg[2].trim().equals("NoPass"))
				desc = activity.getResources().getString(R.string.error_nopass);
			else desc = msg[2];
			final String error = activity.getResources().getString(R.string.error_packet_received) + desc;
			activity.runOnUiThread(new Runnable() {
			    public void run() {
			    	Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
			    }
			});
			return;
		}
		
		final DeviceInfo di = new DeviceInfo();
		di.DeviceName = msg[1].trim();
		di.HostName = msg[2];
		di.RecvPort = recevied_port;
		di.SendPort = activity.getResources().getInteger(R.integer.default_send_port); // that's where we were sending after all
		
		for (int i=6; i<(msg.length-2); i++) {
			String outlet[] = msg[i].split(",");
			if (outlet.length < 1)
				continue;
			OutletInfo oi = new OutletInfo();
			oi.OutletNumber = i-5; // 1-based
			oi.Description = outlet[0];
			if (outlet.length > 1)
				oi.State = outlet[1].equals("1");
			di.Outlets.add(oi);
		}
		
		activity.runOnUiThread(new Runnable() {
			public void run() {
				found_event.onDeviceFound(di);
			}
		});
		
	}
}
