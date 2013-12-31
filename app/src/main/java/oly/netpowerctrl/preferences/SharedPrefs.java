package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.datastructure.OutletInfo;

public class SharedPrefs {

    public final static String PREF_TEMPDEVICE = "oly.netpowerctrl.tempdevice";
    public final static String PREF_BASENAME = "oly.netpowerctrl";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public final static String PREF_GROUPS_BASENAME = "oly.netpowerctrl.groups";
    public final static String PREF_DEVICES = "CONFIGURED_DEVICES";
    public final static String PREF_GROUPS = "GROUPS";
    public final static String PREF_FIRSTTAB = "FIRST_TAB";
    public final static String PREF_UUID = "UUID";
    public final static String PREF_NAME = "NAME";
    public final static String PREF_IP = "IP";
    public final static String PREF_MAC = "MAC";
    public final static String PREF_USERNAME = "USERNAME";
    public final static String PREF_PASSWORD = "PASSWORD";
    public final static String PREF_DEFAULTPORTS = "DEFAULTPORTS";
    public final static String PREF_SENDPORT = "SENDPORT";
    public final static String PREF_RECVPORT = "RECVPORT";
    public final static String PREF_NUM_OUTLETS = "NUM_OUTLETS";
    public final static String PREF_OUTLET_NAME = "OUTLET_NAME";
    public final static String PREF_OUTLET_NUMBER = "OUTLET_NUMBER";

    public static int getFirstTab(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_FIRSTTAB, -1);
    }

    public static void setFirstTab(Context context, int navigationPosition) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putInt(PREF_FIRSTTAB, navigationPosition);
        prefEditor.commit();
    }

    public static ArrayList<OutletCommandGroup> ReadGroups(Context context) {
        ArrayList<OutletCommandGroup> groups = new ArrayList<OutletCommandGroup>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        Set<String> groupSet = prefs.getStringSet(PREF_GROUPS, null);
        if (groupSet == null)
            return groups;

        for (String group_str : groupSet) {
            OutletCommandGroup og = OutletCommandGroup.fromString(group_str, context);
            groups.add(og);

        }
        return groups;
    }

    public static void SaveGroups(ArrayList<OutletCommandGroup> groups, Context context) {
        Set<String> group_str = new TreeSet<String>();

        for (OutletCommandGroup b : groups) {
            group_str.add(b.toString());
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putStringSet(PREF_GROUPS, group_str);
        prefEditor.commit();
    }

    public static String getFullPrefname(String prefname) {
        return PREF_BASENAME + "." + prefname;
    }

    public static ArrayList<DeviceInfo> ReadConfiguredDevices(Context context) {

        ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();

        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        String configured_devices_str = prefs.getString(PREF_DEVICES, "");
        String[] configured_devices = configured_devices_str.split(":");
        for (String device : configured_devices)
            devices.add(ReadDevice(context, getFullPrefname(device)));
        return devices;
    }

    public static DeviceInfo ReadTempDevice(Context context) {
        return ReadDevice(context, PREF_TEMPDEVICE);
    }

    public static DeviceInfo ReadDevice(Context context, String prefname) {
        SharedPreferences device_prefs = context.getSharedPreferences(prefname, Context.MODE_PRIVATE);
        DeviceInfo di = new DeviceInfo(context);
        di.uuid = UUID.fromString(device_prefs.getString(PREF_UUID, UUID.randomUUID().toString()));
        di.DeviceName = device_prefs.getString(PREF_NAME, context.getResources().getText(R.string.default_device_name).toString());
        di.HostName = device_prefs.getString(PREF_IP, "");
        di.MacAddress = device_prefs.getString(PREF_MAC, "");
        di.UserName = device_prefs.getString(PREF_USERNAME, "");
        di.Password = device_prefs.getString(PREF_PASSWORD, "");
        di.DefaultPorts = device_prefs.getBoolean(PREF_DEFAULTPORTS, true);
        if (di.DefaultPorts) {
            di.SendPort = DeviceQuery.getDefaultSendPort(context);
            di.ReceivePort = DeviceQuery.getDefaultRecvPort(context);
        } else {
            di.SendPort = device_prefs.getInt(PREF_SENDPORT, DeviceQuery.getDefaultSendPort(context));
            di.ReceivePort = device_prefs.getInt(PREF_RECVPORT, DeviceQuery.getDefaultRecvPort(context));
        }
        di.Configured = true;
        di.Outlets = new ArrayList<OutletInfo>();

        int num_outlets = device_prefs.getInt(PREF_NUM_OUTLETS, 0);
        for (int i = 0; i < num_outlets; i++) {
            OutletInfo oi = new OutletInfo();
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
            String prefname = getFullPrefname(di.getID());
            SaveDevice(context, prefname, di);
        }

        if (configured_devices.endsWith(":"))
            configured_devices = configured_devices.substring(0, configured_devices.length() - 1);
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
        device_editor.putInt(PREF_RECVPORT, di.ReceivePort);

        device_editor.putInt(PREF_NUM_OUTLETS, di.Outlets.size());
        for (int i = 0; i < di.Outlets.size(); i++) {
            device_editor.putString(PREF_OUTLET_NAME + String.valueOf(i), di.Outlets.get(i).Description);
            device_editor.putInt(PREF_OUTLET_NUMBER + String.valueOf(i), di.Outlets.get(i).OutletNumber);
        }
        device_editor.commit();
    }

    public static void SaveTempDevice(Context context, DeviceInfo di) {
        SaveDevice(context, PREF_TEMPDEVICE, di);
    }

    public static void SaveWidget(Context context, int widgetID, String deviceMac, int outletNumber) {
        final String prefname = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefname, Context.MODE_PRIVATE);
        SharedPreferences.Editor device_editor = device_prefs.edit();
        device_editor.putString(PREF_MAC, deviceMac);
        device_editor.putInt(PREF_OUTLET_NUMBER, outletNumber);
        device_editor.commit();
    }

    public static class UniqueOutlet {
        String deviceMac;
        int outletNumber;
    }

    public static UniqueOutlet LoadWidget(Context context, int widgetID) {
        final String prefname = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefname, Context.MODE_PRIVATE);

        UniqueOutlet result = new UniqueOutlet();
        result.deviceMac = device_prefs.getString(PREF_MAC, null);
        result.outletNumber = device_prefs.getInt(PREF_OUTLET_NUMBER, -1);
        if (result.deviceMac == null)
            return null;
        return result;
    }
}
