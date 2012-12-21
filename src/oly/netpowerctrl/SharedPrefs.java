package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {

	public final static String PREF_BASENAME        = "oly.netpowerctrl";
	public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
	public final static String PREF_DEVICES         = "CONFIGURED_DEVICES";
	public final static String PREF_UUID            = "UUID";
	public final static String PREF_NAME            = "NAME";
	public final static String PREF_IP              = "IP";
	public final static String PREF_MAC             = "MAC";
	public final static String PREF_USERNAME        = "USERNAME";
	public final static String PREF_PASSWORD        = "PASSWORD";
	public final static String PREF_DEFAULTPORTS    = "DEFAULTPORTS";
	public final static String PREF_SENDPORT        = "SENDPORT";
	public final static String PREF_RECVPORT        = "RECVPORT";
	public final static String PREF_NUM_OUTLETS     = "NUM_OUTLETS";
	public final static String PREF_OUTLET_NAME     = "OUTLET_NAME";
	public final static String PREF_OUTLET_NUMBER   = "OUTLET_NUMBER";
	
    public static ArrayList<DeviceInfo> ReadConfiguredDevices(Context context) {

    	ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
    	
		SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
		String configured_devices_str = prefs.getString(PREF_DEVICES, "");
		String[] configured_devices = configured_devices_str.split(":");
		for (String device: configured_devices)
			devices.add(ReadDevice(context, PREF_BASENAME+"."+device));
  		return devices;
    }

	public static DeviceInfo ReadDevice(Context context, String prefname) {
		SharedPreferences device_prefs = context.getSharedPreferences(prefname, Context.MODE_PRIVATE);
		DeviceInfo di = new DeviceInfo(context);
		di.uuid =  UUID.fromString(device_prefs.getString(PREF_UUID, UUID.randomUUID().toString()));
		di.DeviceName = device_prefs.getString(PREF_NAME, context.getResources().getText(R.string.default_device_name).toString());
		di.HostName = device_prefs.getString(PREF_IP,"");
		di.MacAddress = device_prefs.getString(PREF_MAC,"");
		di.UserName= device_prefs.getString(PREF_USERNAME,"");
		di.Password = device_prefs.getString(PREF_PASSWORD,"");
		di.DefaultPorts = device_prefs.getBoolean(PREF_DEFAULTPORTS, true);
		if (di.DefaultPorts) {
			di.SendPort = DeviceQuery.getDefaultSendPort(context);
			di.RecvPort = DeviceQuery.getDefaultRecvPort(context);
		} else {
			di.SendPort = device_prefs.getInt(PREF_SENDPORT, DeviceQuery.getDefaultSendPort(context));
			di.RecvPort = device_prefs.getInt(PREF_RECVPORT, DeviceQuery.getDefaultRecvPort(context));
		}
		di.Outlets = new ArrayList<OutletInfo>();

		int num_outlets = device_prefs.getInt(PREF_NUM_OUTLETS, 0);
		for (int i=0; i<num_outlets; i++) {
			OutletInfo oi = new OutletInfo();
			oi.Description = device_prefs.getString(PREF_OUTLET_NAME+String.valueOf(i), "");
			oi.OutletNumber = device_prefs.getInt(PREF_OUTLET_NUMBER+String.valueOf(i), -1);
			di.Outlets.add(oi);
		}
		return di;
	}
    
    public static void SaveConfiguredDevices(List<DeviceInfo> devices, Context context) {
    	String configured_devices = "";

    	for (DeviceInfo di: devices) {
			configured_devices += di.getPrefname() + ":";
	    	SaveDevice(context, PREF_BASENAME+"."+di.getPrefname(), di);
		}

    	if (configured_devices.endsWith(":"))
    		configured_devices = configured_devices.substring(0, configured_devices.length()-1);
    	SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(PREF_DEVICES, configured_devices);
		prefEditor.commit();
    }

	public static void SaveDevice(Context context, String prefname, DeviceInfo di) {
		SharedPreferences device_prefs = context.getSharedPreferences(prefname, Context.MODE_PRIVATE);
		SharedPreferences.Editor device_editor = device_prefs.edit();
		
		device_editor.putString(PREF_UUID, di.uuid.toString());
		device_editor.putString(PREF_NAME, di.DeviceName);
		device_editor.putString(PREF_IP, di.HostName);
		device_editor.putString(PREF_MAC, di.MacAddress);
		device_editor.putString(PREF_USERNAME, di.UserName);
		device_editor.putString(PREF_PASSWORD, di.Password);
		device_editor.putBoolean(PREF_DEFAULTPORTS, di.DefaultPorts);
		device_editor.putInt(PREF_SENDPORT, di.SendPort);
		device_editor.putInt(PREF_RECVPORT, di.RecvPort);

		device_editor.putInt(PREF_NUM_OUTLETS, di.Outlets.size());
		for (int i=0; i<di.Outlets.size(); i++) {
			device_editor.putString(PREF_OUTLET_NAME+String.valueOf(i), di.Outlets.get(i).Description);
			device_editor.putInt(PREF_OUTLET_NUMBER+String.valueOf(i), di.Outlets.get(i).OutletNumber);
		}
		device_editor.commit();
	}
    

	
}
