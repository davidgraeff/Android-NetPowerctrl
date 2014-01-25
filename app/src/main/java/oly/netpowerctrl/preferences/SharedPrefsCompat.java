package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 * Old device read config
 */
public class SharedPrefsCompat {
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

    public static String getFullPrefName(String prefName) {
        return SharedPrefs.PREF_BASENAME + "." + prefName;
    }

    public static class v0 {
        public static ArrayList<Scene> ReadScenes(Context context) {
            ArrayList<Scene> groups = new ArrayList<Scene>();
            SharedPreferences prefs = context.getSharedPreferences(SharedPrefs.PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
            Set<String> scenes_in_set = prefs.getStringSet(SharedPrefs.PREF_SCENES, null);
            if (scenes_in_set == null)
                return groups;

            for (String group_str : scenes_in_set) {
                Scene og = OutletCommandGroupfromString(group_str);
                groups.add(og);

            }
            return groups;
        }

        public static Scene OutletCommandGroupfromString(String source) {
            if (source == null)
                return null;

            Scene og = new Scene();
            String list_src[] = source.split("§§");
            if (list_src.length == 0)
                return null;

            // sceneName is the first element, uuid the third
            og.sceneName = list_src[0];
            // list_src[1];
            og.uuid = UUID.fromString(list_src[2]);

            for (int i = 3; i < list_src.length; ++i) {
                SceneOutlet c = OutletCommandfromString(list_src[i]);
                if (c != null)
                    og.sceneOutlets.add(c);
            }

            og.sceneDetails = og.buildDetails();

            return og;
        }

        public static SceneOutlet OutletCommandfromString(String source) {
            SceneOutlet c = new SceneOutlet();
            String src[] = source.split("§");
            if (src.length < 3)
                return null;
            c.device_mac = src[1];
            c.outletNumber = Integer.valueOf(src[2]);
            c.state = Integer.valueOf(src[3]);
            c.description = c.device_mac + ":" + Integer.valueOf(c.outletNumber).toString();
            c.outletinfo = NetpowerctrlApplication.instance.findOutlet(c.device_mac, c.outletNumber);
            if (c.outletinfo != null)
                c.description = c.outletinfo.device.DeviceName + ": " + c.outletinfo.getDescription();
            return c;
        }

        public static ArrayList<DeviceInfo> ReadConfiguredDevices(Context context) {
            ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();

            SharedPreferences prefs = context.getSharedPreferences(SharedPrefs.PREF_BASENAME, Context.MODE_PRIVATE);
            String configured_devices_str = prefs.getString(SharedPrefs.PREF_DEVICES, "");
            if (configured_devices_str.length() == 0)
                return devices;
            String[] configured_devices = configured_devices_str.split(":");
            for (String device : configured_devices)
                devices.add(ReadDevice(context, getFullPrefName(device)));
            return devices;
        }

        public static DeviceInfo ReadDevice(Context context, String prefName) {
            SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            DeviceInfo di = DeviceInfo.createNewDevice();
            di.uuid = UUID.fromString(device_prefs.getString(PREF_UUID, UUID.randomUUID().toString()));
            di.DeviceName = device_prefs.getString(PREF_NAME, context.getString(R.string.default_device_name));
            di.HostName = device_prefs.getString(PREF_IP, "");
            di.MacAddress = device_prefs.getString(PREF_MAC, "");
            di.UserName = device_prefs.getString(PREF_USERNAME, "");
            di.Password = device_prefs.getString(PREF_PASSWORD, "");
            di.DefaultPorts = device_prefs.getBoolean(PREF_DEFAULT_PORTS, true);
            if (di.DefaultPorts) {
                di.SendPort = SharedPrefs.getDefaultSendPort();
                di.ReceivePort = SharedPrefs.getDefaultReceivePort();
            } else {
                di.SendPort = device_prefs.getInt(PREF_SEND_PORT, SharedPrefs.getDefaultSendPort());
                di.ReceivePort = device_prefs.getInt(PREF_RECEIVE_PORT, SharedPrefs.getDefaultReceivePort());
            }
            di.Outlets = new ArrayList<OutletInfo>();

            int num_outlets = device_prefs.getInt(PREF_NUM_OUTLETS, 0);
            for (int i = 0; i < num_outlets; i++) {
                OutletInfo oi = new OutletInfo(di);
                oi.setDescriptionByDevice(device_prefs.getString(PREF_OUTLET_NAME + String.valueOf(i), ""));
                oi.OutletNumber = device_prefs.getInt(PREF_OUTLET_NUMBER + String.valueOf(i), -1);
                di.Outlets.add(oi);
            }
            return di;
        }
    }

}
