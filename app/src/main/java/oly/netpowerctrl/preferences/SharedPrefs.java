package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.datastructure.OutletInfo;

public class SharedPrefs {

    public final static String PREF_TEMP_DEVICE = "oly.netpowerctrl.tempdevice";
    public final static String PREF_BASENAME = "oly.netpowerctrl";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public final static String PREF_GROUPS_BASENAME = "oly.netpowerctrl.groups";
    public final static String PREF_DEVICES = "CONFIGURED_DEVICES";
    public final static String PREF_SCENES = "GROUPS";
    public final static String PREF_FIRST_TAB = "FIRST_TAB";
    public final static String PREF_UUID = "UUID";
    public final static String PREF_NAME = "NAME";
    public final static String PREF_IP = "IP";
    public final static String PREF_MAC = "MAC";
    public final static String PREF_USERNAME = "USERNAME";
    public final static String PREF_PASSWORD = "PASSWORD";
    public final static String PREF_DEFAULT_PORTS = "DEFAULTPORTS";
    public final static String PREF_SEND_PORT = "SENDPORT";
    public final static String PREF_RECEIVE_PORT = "RECVPORT";
    public final static String PREF_NUM_OUTLETS = "NUM_OUTLETS";
    public final static String PREF_OUTLET_NAME = "OUTLET_NAME";
    public final static String PREF_OUTLET_NUMBER = "OUTLET_NUMBER";

    public static int getFirstTab(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        int tab = 0;
        try {
            tab = prefs.getInt(PREF_FIRST_TAB, 0);
        } catch (ClassCastException ignored) {

        }
        return tab;
    }

    public static void setFirstTab(Context context, int navigationPosition) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putInt(PREF_FIRST_TAB, navigationPosition);
        prefEditor.commit();
    }

    public static ArrayList<OutletCommandGroup> ReadScenes(Context context) {
        ArrayList<OutletCommandGroup> groups = new ArrayList<OutletCommandGroup>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        Set<String> scenes_in_set = prefs.getStringSet(PREF_SCENES, null);
        if (scenes_in_set == null)
            return groups;

        for (String group_str : scenes_in_set) {
            OutletCommandGroup og = OutletCommandGroup.fromString(group_str, context);
            groups.add(og);

        }
        return groups;
    }

    public static void SaveScenes(ArrayList<OutletCommandGroup> scenes, Context context) {
        Set<String> scenes_in_set = new TreeSet<String>();

        for (OutletCommandGroup scene : scenes) {
            scenes_in_set.add(scene.toString());
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putStringSet(PREF_SCENES, scenes_in_set);
        prefEditor.commit();
    }

    public static String getFullPrefName(String prefName) {
        return PREF_BASENAME + "." + prefName;
    }

    public static ArrayList<DeviceInfo> ReadConfiguredDevices(Context context) {

        ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();

        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        String configured_devices_str = prefs.getString(PREF_DEVICES, "");
        String[] configured_devices = configured_devices_str.split(":");
        for (String device : configured_devices)
            devices.add(ReadDevice(context, getFullPrefName(device)));
        return devices;
    }

    public static DeviceInfo ReadTempDevice(Context context) {
        return ReadDevice(context, PREF_TEMP_DEVICE);
    }

    public static DeviceInfo ReadDevice(Context context, String prefName) {
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        DeviceInfo di = new DeviceInfo(context);
        di.uuid = UUID.fromString(device_prefs.getString(PREF_UUID, UUID.randomUUID().toString()));
        di.DeviceName = device_prefs.getString(PREF_NAME, context.getResources().getText(R.string.default_device_name).toString());
        di.HostName = device_prefs.getString(PREF_IP, "");
        di.MacAddress = device_prefs.getString(PREF_MAC, "");
        di.UserName = device_prefs.getString(PREF_USERNAME, "");
        di.Password = device_prefs.getString(PREF_PASSWORD, "");
        di.DefaultPorts = device_prefs.getBoolean(PREF_DEFAULT_PORTS, true);
        if (di.DefaultPorts) {
            di.SendPort = getDefaultSendPort(context);
            di.ReceivePort = getDefaultReceivePort(context);
        } else {
            di.SendPort = device_prefs.getInt(PREF_SEND_PORT, getDefaultSendPort(context));
            di.ReceivePort = device_prefs.getInt(PREF_RECEIVE_PORT, getDefaultReceivePort(context));
        }
        di.Configured = true;
        di.Outlets = new ArrayList<OutletInfo>();

        int num_outlets = device_prefs.getInt(PREF_NUM_OUTLETS, 0);
        for (int i = 0; i < num_outlets; i++) {
            OutletInfo oi = new OutletInfo(di);
            oi.Description = device_prefs.getString(PREF_OUTLET_NAME + String.valueOf(i), "");
            oi.OutletNumber = device_prefs.getInt(PREF_OUTLET_NUMBER + String.valueOf(i), -1);
            di.Outlets.add(oi);
        }
        return di;
    }

    public static void SaveConfiguredDevices(List<DeviceInfo> devices, Context context) {
        String configured_devices = "";

        for (DeviceInfo di : devices) {
            configured_devices += di.getID() + ":";
            SaveDevice(context, getFullPrefName(di.getID()), di);
        }

        if (configured_devices.endsWith(":"))
            configured_devices = configured_devices.substring(0, configured_devices.length() - 1);
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(PREF_DEVICES, configured_devices);
        prefEditor.commit();
    }

    public static void SaveDevice(Context context, String prefName, DeviceInfo di) {
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor device_editor = device_prefs.edit();

        device_editor.putString(PREF_UUID, di.uuid.toString());
        device_editor.putString(PREF_NAME, di.DeviceName);
        device_editor.putString(PREF_IP, di.HostName);
        device_editor.putString(PREF_MAC, di.MacAddress);
        device_editor.putString(PREF_USERNAME, di.UserName);
        device_editor.putString(PREF_PASSWORD, di.Password);
        device_editor.putBoolean(PREF_DEFAULT_PORTS, di.DefaultPorts);
        device_editor.putInt(PREF_SEND_PORT, di.SendPort);
        device_editor.putInt(PREF_RECEIVE_PORT, di.ReceivePort);

        device_editor.putInt(PREF_NUM_OUTLETS, di.Outlets.size());
        for (int i = 0; i < di.Outlets.size(); i++) {
            device_editor.putString(PREF_OUTLET_NAME + String.valueOf(i), di.Outlets.get(i).Description);
            device_editor.putInt(PREF_OUTLET_NUMBER + String.valueOf(i), di.Outlets.get(i).OutletNumber);
        }
        device_editor.commit();
    }

    public static void SaveTempDevice(Context context, DeviceInfo di) {
        SaveDevice(context, PREF_TEMP_DEVICE, di);
    }

    public static void SaveWidget(Context context, int widgetID, String deviceMac, int outletNumber) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor device_editor = device_prefs.edit();
        device_editor.putString(PREF_MAC, deviceMac);
        device_editor.putInt(PREF_OUTLET_NUMBER, outletNumber);
        device_editor.commit();
    }

    public static void DeleteWidgets(Context context, int appWidgetId) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().commit();
    }

    public static class UniqueOutlet {
        public String deviceMac;
        public int outletNumber;
    }

    public static UniqueOutlet LoadWidget(Context context, int widgetID) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

        UniqueOutlet result = new UniqueOutlet();
        result.deviceMac = device_prefs.getString(PREF_MAC, null);
        result.outletNumber = device_prefs.getInt(PREF_OUTLET_NUMBER, -1);
        if (result.deviceMac == null)
            return null;
        return result;
    }


    public static int getDefaultSendPort(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try {
            send_udp = Integer.parseInt(prefs.getString("standard_send_port", ""));
        } catch (NumberFormatException e) { /*nop*/ }
        return send_udp;
    }

    public static int getDefaultReceivePort(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int receive_port_udp = context.getResources().getInteger(R.integer.default_recv_port);
        try {
            receive_port_udp = Integer.parseInt(prefs.getString("standard_recv_port", ""));
        } catch (NumberFormatException e) { /*nop*/ }
        return receive_port_udp;
    }

}
