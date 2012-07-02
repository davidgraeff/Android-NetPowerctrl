package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class SharedPrefs {

    public static ArrayList<DeviceInfo> ReadConfiguredDevices(Activity activity) {

    	ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
    	
		SharedPreferences prefs = activity.getSharedPreferences("oly.netpowerctrl", Context.MODE_PRIVATE);
		String configured_devices_str = prefs.getString("configured_devices", "[]");
  		try {
			JSONArray jdevices = new JSONArray(configured_devices_str);
			
			for (int i=0; i<jdevices.length(); i++) {
				JSONObject jhost = jdevices.getJSONObject(i);
				DeviceInfo di = new DeviceInfo(activity);
				di.uuid = UUID.fromString(jhost.getString("uuid"));
				di.DeviceName = jhost.getString("name");
				di.HostName = jhost.getString("ip");
				di.MacAddress = jhost.getString("mac");
				di.UserName= jhost.getString("username");
				di.Password = jhost.getString("password");
				di.DefaultPorts = jhost.getBoolean("default_ports");
				if (di.DefaultPorts) {
					di.SendPort = DeviceQuery.getDefaultSendPort(activity);
					di.RecvPort = DeviceQuery.getDefaultRecvPort(activity);
				} else {
					di.SendPort = jhost.getInt("sendport");
					di.RecvPort = jhost.getInt("recvport");
				}
				di.Outlets = new ArrayList<OutletInfo>();

				JSONArray joutlets = jhost.getJSONArray("outlets");
				for (int j=0; j<joutlets.length(); j++) {
					JSONObject joutlet = joutlets.getJSONObject(j);
					OutletInfo oi = new OutletInfo();
					oi.OutletNumber = joutlet.getInt("number");
					oi.Description = joutlet.getString("description");
					di.Outlets.add(oi);
				}

				devices.add(di);
			}
		}
		catch (JSONException e) {
			Toast.makeText(activity, activity.getResources().getText(R.string.error_reading_configured_devices) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
  		return devices;
    }
    
    public static void SaveConfiguredDevices(List<DeviceInfo> devices, Activity activity) {
    	JSONArray jdevices = new JSONArray();
  		try {
  			for (DeviceInfo di: devices) {
				JSONObject jhost = new JSONObject();
				jhost.put("uuid", di.uuid.toString());
				jhost.put("name", di.DeviceName);
				jhost.put("ip", di.HostName);
				jhost.put("mac", di.MacAddress);
				jhost.put("username", di.UserName);
				jhost.put("password", di.Password);
				jhost.put("default_ports", di.DefaultPorts);
				jhost.put("sendport", di.SendPort);
				jhost.put("recvport", di.RecvPort);

				JSONArray joutlets = new JSONArray();
	  			for (OutletInfo oi: di.Outlets) {
					JSONObject joutlet = new JSONObject();
					joutlet.put("number", oi.OutletNumber);
					joutlet.put("description", oi.Description);
					joutlets.put(joutlet);
				}
	  			jhost.put("outlets", joutlets);
	  			jdevices.put(jhost);
  			}
		}
		catch (JSONException e) {
			Toast.makeText(activity, activity.getResources().getText(R.string.error_saving_configured_devices) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}
		SharedPreferences prefs = activity.getSharedPreferences("oly.netpowerctrl", Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("configured_devices", jdevices.toString());
		prefEditor.commit();
    }
    

	
}
