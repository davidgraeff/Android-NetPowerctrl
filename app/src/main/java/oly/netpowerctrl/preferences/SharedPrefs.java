package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.utils.JSONHelper;

public class SharedPrefs {
    public final static int PREF_CURRENT_VERSION = 1;
    public final static String PREF_VERSION_DEVICES = "version_devices";
    public final static String PREF_VERSION_SCENES = "version_scenes";
    public final static String PREF_BASENAME = "oly.netpowerctrl";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public final static String PREF_GROUPS_BASENAME = "oly.netpowerctrl.groups";
    public final static String PREF_DEVICES = "CONFIGURED_DEVICES";
    public final static String PREF_SCENES = "GROUPS";
    public final static String PREF_FIRST_TAB = "FIRST_TAB";
    public final static String PREF_OUTLET_NUMBER = "OUTLET_NUMBER";
    public final static String PREF_MAC = "MAC";
    public final static String PREF_standard_send_port = "standard_send_port";
    public final static String PREF_standard_receive_port = "standard_receive_port";
    public final static String PREF_keep_widget_service_running = "keep_widget_service_running";
    public final static String PREF_use_dark_theme = "use_dark_theme";

    public static String getFirstTab(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        String tab = "";
        try {
            tab = prefs.getString(PREF_FIRST_TAB, "");
        } catch (ClassCastException ignored) {
        }
        return tab;
    }

    public static void setFirstTab(Context context, String fragmentClassName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(PREF_FIRST_TAB, fragmentClassName);
        prefEditor.commit();
    }

    public static List<Scene> ReadScenes(Context context) {
        // Read deprecated scenes
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        int prefVersion = prefs.getInt(PREF_VERSION_SCENES, 0);
        if (prefVersion == 0) {
            return SharedPrefsCompat.v0.ReadScenes(context);
        }

        String scenes_str = prefs.getString(PREF_SCENES, "");

        try {
            return SceneCollection.fromJSON(JSONHelper.getReader(scenes_str)).scenes;
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_reading_scenes), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public static void SaveScenes(List<Scene> scenes, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        try {
            JSONHelper h = new JSONHelper();
            SceneCollection.fromScenes(scenes).toJSON(h.createWriter());

            prefEditor.putInt(PREF_VERSION_SCENES, PREF_CURRENT_VERSION);
            prefEditor.putString(PREF_SCENES, h.getString());
            prefEditor.commit();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_saving_scenes), Toast.LENGTH_SHORT).show();
        }
    }

    public static List<DeviceInfo> ReadConfiguredDevices(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SharedPrefs.PREF_BASENAME, Context.MODE_PRIVATE);

        // Read deprecated configurations
        int prefVersion = prefs.getInt(PREF_VERSION_DEVICES, 0);
        if (prefVersion == 0) {
            return SharedPrefsCompat.v0.ReadConfiguredDevices(context);
        }

        String configured_devices_str = prefs.getString(PREF_DEVICES, "");

        try {
            return DeviceCollection.fromJSON(JSONHelper.getReader(configured_devices_str)).devices;
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.error_reading_configured_devices) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return new ArrayList<DeviceInfo>();
    }

    public static void SaveConfiguredDevices(List<DeviceInfo> devices, Context context) {
        String configured_devices = null;
        try {
            JSONHelper h = new JSONHelper();
            DeviceCollection.fromDevices(devices).toJSON(h.createWriter());
            configured_devices = h.getString();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_saving_configured_devices), Toast.LENGTH_SHORT).show();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_VERSION_DEVICES, PREF_CURRENT_VERSION)
                .putString(PREF_DEVICES, configured_devices).commit();
    }

    public static boolean getShowHiddenOutlets(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return prefs.getBoolean("showHiddenOutlets", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean getShowDeviceNames(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return prefs.getBoolean("showDeviceNames", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setShowDeviceNames(boolean showDeviceNames, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("showDeviceNames", showDeviceNames).commit();
    }

    public static void setShowHiddenOutlets(boolean showHiddenOutlets, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("showHiddenOutlets", showHiddenOutlets).commit();
    }

    public static void savePlugins(Set<String> pluginServiceNameList, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet("plugins", pluginServiceNameList).commit();
    }

    public static Set<String> readPlugins(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet("plugins", null);
    }

    public static class WidgetOutlet {
        public String deviceMac;
        public int outletNumber;

        public WidgetOutlet(String deviceMac, int outletNumber) {
            this.deviceMac = deviceMac;
            this.outletNumber = outletNumber;
        }
    }

    public static void SaveWidget(Context context, int widgetID, WidgetOutlet wo) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor device_editor = device_prefs.edit();
        device_editor.putString(PREF_MAC, wo.deviceMac);
        device_editor.putInt(PREF_OUTLET_NUMBER, wo.outletNumber);
        device_editor.commit();
    }

    public static void DeleteWidgets(Context context, int appWidgetId) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().commit();
    }

    public static WidgetOutlet LoadWidget(Context context, int widgetID) {
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

        WidgetOutlet result = new WidgetOutlet(device_prefs.getString(PREF_MAC, null),
                device_prefs.getInt(PREF_OUTLET_NUMBER, -1));
        if (result.deviceMac == null)
            return null;
        return result;
    }

    public static int getDefaultSendPort(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try {
            send_udp = Integer.parseInt(prefs.getString(PREF_standard_send_port, ""));
        } catch (NumberFormatException e) { /*nop*/ }
        return send_udp;
    }

    public static int getDefaultReceivePort(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int receive_port_udp = context.getResources().getInteger(R.integer.default_receive_port);
        try {
            receive_port_udp = Integer.parseInt(prefs.getString(PREF_standard_receive_port, ""));
        } catch (NumberFormatException e) { /*nop*/ }
        return receive_port_udp;
    }

    public static boolean getKeepWidgetServiceOn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean keep_widget_service_running = context.getResources().getBoolean(R.bool.keep_widget_service_running);
        try {
            keep_widget_service_running = prefs.getBoolean(PREF_keep_widget_service_running, false);
        } catch (Exception e) { /*nop*/ }
        return keep_widget_service_running;
    }

    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.use_dark_theme);
        try {
            value = prefs.getBoolean(PREF_use_dark_theme, false);
        } catch (NumberFormatException e) { /*nop*/ }
        return value;
    }

}
