package oly.netpowerctrl;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
				} catch (final Exception e) {
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
		for (int port: getAllSendPorts(activity)) 
			sendQuery(activity, "255.255.255.255", port);
	}
	

	public static int getDefaultSendPort(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try { send_udp = Integer.parseInt(prefs.getString("standard_send_port", "")); } catch (NumberFormatException e) { /*nop*/ }
        return send_udp;
	}

	public static int getDefaultRecvPort(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int recv_udp = context.getResources().getInteger(R.integer.default_recv_port);
        try { recv_udp = Integer.parseInt(prefs.getString("standard_recv_port", "")); } catch (NumberFormatException e) { /*nop*/ }
        return recv_udp;
	}
	
	//! get a list of all send ports of all configured devices plus the default send port
	public static ArrayList<Integer> getAllSendPorts(Context context) {
		HashSet<Integer> ports = new HashSet<Integer>();
		ports.add(getDefaultSendPort(context));
		
		SharedPreferences prefs = context.getSharedPreferences("oly.netpowerctrl", Context.MODE_PRIVATE);
		String configured_devices_str = prefs.getString("configured_devices", "[]");
  		try {
			JSONArray jdevices = new JSONArray(configured_devices_str);
			for (int i=0; i<jdevices.length(); i++) {
				JSONObject jhost = jdevices.getJSONObject(i);
				if (! jhost.getBoolean("default_ports"))
					ports.add(jhost.getInt("sendport"));
			}
  		}
		catch (JSONException e) {
			// nop
		}
		
		return new ArrayList<Integer>(ports);
	}

	//! get a list of all receive ports of all configured devices plus the default receive port
	public static ArrayList<Integer> getAllReceivePorts(Context context) {
		HashSet<Integer> ports = new HashSet<Integer>();
		ports.add(getDefaultRecvPort(context));
		
		SharedPreferences prefs = context.getSharedPreferences("oly.netpowerctrl", Context.MODE_PRIVATE);
		String configured_devices_str = prefs.getString("configured_devices", "[]");
  		try {
			JSONArray jdevices = new JSONArray(configured_devices_str);
			for (int i=0; i<jdevices.length(); i++) {
				JSONObject jhost = jdevices.getJSONObject(i);
				if (! jhost.getBoolean("default_ports"))
					ports.add(jhost.getInt("recvport"));
			}
  		}
		catch (JSONException e) {
			// nop
		}
		
		return new ArrayList<Integer>(ports);
	}

}
