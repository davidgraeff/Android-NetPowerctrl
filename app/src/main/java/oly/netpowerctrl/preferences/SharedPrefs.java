package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Groups;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.utils.JSONHelper;

public class SharedPrefs {
    public final static int PREF_CURRENT_VERSION = 2;
    public final static String PREF_VERSION_DEVICES = "version_devices";
    public final static String PREF_VERSION_SCENES = "version_scenes";
    public final static String PREF_BASENAME = "oly.netpowerctrl";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public final static String PREF_GROUPS_BASENAME = "oly.netpowerctrl.groups";
    public final static String PREF_DEVICES = "CONFIGURED_DEVICES";
    public final static String PREF_SCENES = "GROUPS";
    public final static String PREF_FIRST_TAB = "FIRST_TAB";
    public final static String PREF_UUID = "UUID";
    public final static String PREF_standard_send_port = "standard_send_port";
    public final static String PREF_standard_receive_port = "standard_receive_port";
    public final static String PREF_keep_widget_service_running = "keep_widget_service_running";
    public final static String PREF_use_dark_theme = "use_dark_theme";
    public final static String PREF_load_plugins = "load_plugins";
    public final static String PREF_widgets = "widgets";
    public final static String PREF_notify_on_stop = "notify_on_stop";

    public static String getFirstTab() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        String tab = "";
        try {
            tab = prefs.getString(PREF_FIRST_TAB, "");
        } catch (ClassCastException ignored) {
        }
        return tab;
    }

    public static void setFirstTab(String fragmentClassName) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = context.getSharedPreferences(PREF_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(PREF_FIRST_TAB, fragmentClassName);
        prefEditor.commit();
    }

    public static SceneCollection ReadScenes() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        int prefVersion = prefs.getInt(PREF_VERSION_SCENES, 0);
        // Read deprecated scenes
        if (prefVersion < PREF_CURRENT_VERSION) {
            Toast.makeText(context, context.getString(R.string.error_reading_scenes_old), Toast.LENGTH_LONG).show();
            SceneCollection sceneCollection = new SceneCollection(new SceneCollection.IScenesSave() {
                @Override
                public void scenesSave(SceneCollection scenes) {
                    SaveScenes(scenes);
                }
            });
            SaveScenes(sceneCollection);
            return sceneCollection;
        }

        String scenes_str = prefs.getString(PREF_SCENES, "");

        try {
            return SceneCollection.fromJSON(JSONHelper.getReader(scenes_str), new SceneCollection.IScenesSave() {
                @Override
                public void scenesSave(SceneCollection scenes) {
                    SaveScenes(scenes);
                }
            });
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_reading_scenes), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public static void SaveScenes(SceneCollection scenes) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = context.getSharedPreferences(PREF_GROUPS_BASENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        try {
            JSONHelper h = new JSONHelper();
            scenes.toJSON(h.createWriter());

            prefEditor.putInt(PREF_VERSION_SCENES, PREF_CURRENT_VERSION);
            prefEditor.putString(PREF_SCENES, h.getString());
            prefEditor.commit();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_saving_scenes), Toast.LENGTH_SHORT).show();
        }
    }

    public static List<DeviceInfo> ReadConfiguredDevices() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = context.getSharedPreferences(SharedPrefs.PREF_BASENAME, Context.MODE_PRIVATE);

        // Read deprecated configurations
        int prefVersion = prefs.getInt(PREF_VERSION_DEVICES, 0);
        if (prefVersion < PREF_CURRENT_VERSION) {
            return new ArrayList<DeviceInfo>();
        }

        String configured_devices_str = prefs.getString(PREF_DEVICES, "");

        try {
            return DeviceCollection.fromJSON(JSONHelper.getReader(configured_devices_str)).devices;
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.error_reading_configured_devices) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return new ArrayList<DeviceInfo>();
    }

    public static void SaveConfiguredDevices(List<DeviceInfo> devices) {
        Context context = NetpowerctrlApplication.instance;
        String configured_devices = null;
        try {
            JSONHelper h = new JSONHelper();
            DeviceCollection.fromDevices(devices).toJSON(h.createWriter());
            configured_devices = h.getString();
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.error_saving_configured_devices), Toast.LENGTH_SHORT).show();
        }

        for (DeviceInfo di : devices)
            di.configured = true;

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

    public static void setShowHiddenOutlets(boolean showHiddenOutlets) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("showHiddenOutlets", showHiddenOutlets).commit();
    }

    public static void saveGroups(Groups groups) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("groups", groups.toJSON()).commit();
    }

    public static Groups readGroups() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Groups.fromJSON(JSONHelper.getReader(prefs.getString("groups", "")));
        } catch (IOException e) {
            return new Groups();
        }
    }

    public static boolean getLoadExtensions() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean keep_widget_service_running = context.getResources().getBoolean(R.bool.load_plugins);
        try {
            keep_widget_service_running = prefs.getBoolean(PREF_load_plugins, false);
        } catch (Exception e) { /*nop*/ }
        return keep_widget_service_running;
    }

    public static void SaveWidget(int widgetID, String devicePortUuid) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor device_editor = device_prefs.edit();
        device_editor.putString(PREF_UUID, devicePortUuid);
        device_editor.commit();
    }

    public static void DeleteWidgets(int appWidgetId) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().commit();
    }

    public static String LoadWidget(int widgetID) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

        return device_prefs.getString(PREF_UUID, null);
    }

    public static int getDefaultSendPort() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try {
            send_udp = Integer.parseInt(prefs.getString(PREF_standard_send_port, Integer.valueOf(send_udp).toString()));
        } catch (NumberFormatException e) { /*nop*/ }
        return send_udp;
    }

    public static int getDefaultReceivePort() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int receive_port_udp = context.getResources().getInteger(R.integer.default_receive_port);
        try {
            receive_port_udp = Integer.parseInt(prefs.getString(PREF_standard_receive_port, Integer.valueOf(receive_port_udp).toString()));
        } catch (NumberFormatException e) { /*nop*/ }
        return receive_port_udp;
    }

    public static boolean getKeepWidgetServiceOn() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean keep_widget_service_running = context.getResources().getBoolean(R.bool.keep_widget_service_running);
        try {
            keep_widget_service_running = prefs.getBoolean(PREF_keep_widget_service_running, keep_widget_service_running);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return keep_widget_service_running;
    }

    public static boolean isDarkTheme() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.use_dark_theme);
        try {
            value = prefs.getBoolean(PREF_use_dark_theme, value);
        } catch (NumberFormatException e) { /*nop*/ }
        return value;
    }

    public static boolean notifyOnStop() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.notify_on_stop);
        try {
            value = prefs.getBoolean(PREF_notify_on_stop, value);
        } catch (NumberFormatException e) { /*nop*/ }
        return value;
    }

    public static int getMaxFavScenes() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int value = context.getResources().getInteger(R.integer.max_favourites_scenes);
        try {
            value = Integer.parseInt(prefs.getString("max_favourite_scenes", Integer.valueOf(value).toString()));
        } catch (NumberFormatException e) { /*nop*/ }
        return value;
    }

    public static int getMaxFavGroups() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int value = context.getResources().getInteger(R.integer.max_favourites_groups);
        try {
            value = Integer.parseInt(prefs.getString("max_favourite_groups", Integer.valueOf(value).toString()));
        } catch (NumberFormatException e) { /*nop*/ }
        return value;
    }
}
