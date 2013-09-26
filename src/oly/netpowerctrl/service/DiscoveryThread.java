package oly.netpowerctrl.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.OutletInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class DiscoveryThread extends Thread {
	
	public static String BROADCAST_DEVICE_DISCOVERED = "com.nittka.netpowerctrl.DEVICE_DISCOVERED";
	public static String BROADCAST_RESTART_DISCOVERY = "com.nittka.netpowerctrl.RESTART_DISCOVERY";
	
	int recv_port;
	Context ctx;
	boolean keep_running;
	DatagramSocket socket;
	
	public DiscoveryThread(int port, Context context) {
		recv_port = port;
		ctx = context;
		socket = null;
	}
	
	public void run() {
		
		keep_running = true;
		while (keep_running) {
			try {
				byte[] message = new byte[1500];
		        DatagramPacket p = new DatagramPacket(message, message.length);
		        socket = new DatagramSocket(recv_port);
		        socket.setReuseAddress(true);
				while (keep_running) {
					socket.receive(p);
		        	parsePacket(new String(message, 0, p.getLength(), "latin-1"), recv_port);
				}
				socket.close();
			} catch (final IOException e) {
				if (keep_running) { // no message if we were interrupt()ed
			    	String msg = String.format(ctx.getResources().getString(R.string.error_listen_thread_exception), recv_port);
			    	msg += e.getLocalizedMessage();
			    	if (recv_port < 1024) msg += ctx.getResources().getString(R.string.error_port_lt_1024);
			    	ShowToast(msg);
				}
				break;
			}
		}
	}

	@Override
	public void interrupt() {
	    keep_running = false;
	    if (socket != null) {
	    	socket.close();
	    }	    	
	    super.interrupt();
	}

	public void parsePacket(final String message, int recevied_port) {
		
		String msg[] = message.split(":");
		if (msg.length < 3)
			return;
		
		if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
			// error packet received
			String desc;
			if (msg[2].trim().equals("NoPass"))
				desc = ctx.getResources().getString(R.string.error_nopass);
			else desc = msg[2];
			String error = ctx.getResources().getString(R.string.error_packet_received) + desc;
			ShowToast(error);
			return;
		}
		
		final DeviceInfo di = new DeviceInfo(ctx);
		di.DeviceName = msg[1].trim();
		di.HostName = msg[2];
		di.MacAddress = msg[5];
		di.RecvPort = recevied_port;
		// leave SendPort as default, as we have no way to know.
		
		int disabledOutlets = 0;
		int numOutlets = 8; // normally, the device sends info for 8 outlets no matter how many are actually equipped
		
		if (msg.length > 14)
			try {disabledOutlets = Integer.parseInt(msg[14]);} catch (NumberFormatException e) {}
			
		if (msg.length < 14)
			numOutlets = msg.length-6;
		
		for (int i=0; i<numOutlets; i++) {
			String outlet[] = msg[6+i].split(",");
			if (outlet.length < 1)
				continue;
			OutletInfo oi = new OutletInfo();
			oi.OutletNumber = i+1; // 1-based
			oi.Description = outlet[0];
			if (outlet.length > 1)
				oi.State = outlet[1].equals("1");
			oi.Disabled = (disabledOutlets & (1<<i)) != 0; 

			di.Outlets.add(oi);
		}
		
		Intent it = new Intent(BROADCAST_DEVICE_DISCOVERED);
		it.putExtra("device_info", di);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(it);
	}
	
	public void ShowToast(final String message) {
	  Handler h = new Handler(ctx.getMainLooper());

	    h.post(new Runnable() {
	        public void run() {
	             Toast.makeText(ctx,message,Toast.LENGTH_LONG).show();
	        }
	    });
	}
}
