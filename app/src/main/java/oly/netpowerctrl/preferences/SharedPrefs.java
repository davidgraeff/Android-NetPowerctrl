package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class SharedPrefs {
    public final static String PREF_widgets = "widgets";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public static final String PREF_use_dark_theme = "use_dark_theme";
    public static final String PREF_show_persistent_notification = "show_persistent_notification";
    public final static String hide_not_reachable = "hide_not_reachable";
    private final static int PREF_CURRENT_VERSION = 3;

    public static int getLastPreferenceVersion() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int lastSavedVersion = -1;
        try {
            lastSavedVersion = prefs.getInt("prefVersion", lastSavedVersion);
        } catch (Exception ignored) {
        }
        return lastSavedVersion;
    }

    public static void setCurrentPreferenceVersion() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("prefVersion", PREF_CURRENT_VERSION).apply();
    }

    public static String getFirstTab() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String tab = "";
        try {
            tab = prefs.getString("FIRST_TAB", "");
        } catch (Exception ignored) {
        }
        return tab;
    }

    public static void setFirstTab(String fragmentClassName) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString("FIRST_TAB", fragmentClassName);
        prefEditor.apply();
    }

    public static boolean getShowHiddenOutlets() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean ShowHiddenOutlets = context.getResources().getBoolean(R.bool.showHiddenOutlets);
        try {
            ShowHiddenOutlets = prefs.getBoolean("showHiddenOutlets", ShowHiddenOutlets);
        } catch (Exception ignored) {
        }
        return ShowHiddenOutlets;
    }

    public static void setShowHiddenOutlets(boolean showHiddenOutlets) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("showHiddenOutlets", showHiddenOutlets).apply();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean getLoadExtensions() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean keep_widget_service_running = context.getResources().getBoolean(R.bool.load_extensions);
        try {
            keep_widget_service_running = prefs.getBoolean("load_extensions", false);
        } catch (Exception ignored) {
        }
        return keep_widget_service_running;
    }

    public static void SaveWidget(int widgetID, String devicePortUuid) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().putString("UUID", devicePortUuid).apply();
    }

    public static void DeleteWidgets(int appWidgetId) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().apply();
    }

    public static String LoadWidget(int widgetID) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return device_prefs.getString("UUID", null);
    }

    public static int getDefaultSendPort() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try {
            send_udp = Integer.parseInt(prefs.getString("standard_send_port", Integer.valueOf(send_udp).toString()));
        } catch (Exception ignored) {
        }
        return send_udp;
    }

    public static int getDefaultReceivePort() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int receive_port_udp = context.getResources().getInteger(R.integer.default_receive_port);
        try {
            receive_port_udp = Integer.parseInt(prefs.getString("standard_receive_port", Integer.valueOf(receive_port_udp).toString()));
        } catch (Exception ignored) {
        }
        return receive_port_udp;
    }

    public static boolean isEnergySavingEnabled() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean use_energy_saving_mode = context.getResources().getBoolean(R.bool.use_energy_saving_mode);
        try {
            use_energy_saving_mode = prefs.getBoolean("use_energy_saving_mode", use_energy_saving_mode);
        } catch (Exception ignored) {
        }
        return use_energy_saving_mode;
    }

    public static boolean isWakeUpFromEnergySaving() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wakeUp_energy_saving_mode = context.getResources().getBoolean(R.bool.wakeup_energy_saving_mode);
        try {
            wakeUp_energy_saving_mode = prefs.getBoolean("wakeup_energy_saving_mode", wakeUp_energy_saving_mode);
        } catch (Exception ignored) {
        }
        return wakeUp_energy_saving_mode;
    }

    public static boolean isDarkTheme() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.use_dark_theme);
        try {
            value = prefs.getBoolean(PREF_use_dark_theme, value);
        } catch (Exception ignored) {
        }
        return value;
    }

    public static boolean notifyOnStop() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.notify_on_stop);
        try {
            value = prefs.getBoolean("notify_on_stop", value);
        } catch (Exception ignored) {
        }
        return value;
    }

    public static int getMaxFavGroups() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int value = context.getResources().getInteger(R.integer.max_favourites_groups);
        try {
            value = Integer.parseInt(prefs.getString("max_favourite_groups", Integer.valueOf(value).toString()));
        } catch (Exception ignored) {
        }
        return value;
    }

    public static boolean getOutletsGrid() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("OutletsGrid", false);
    }

    public static void setOutletsGrid(boolean grid) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("OutletsGrid", grid).apply();
    }

    public static boolean getScenesList() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("ScenesList", false);
    }

    public static void setScenesList(boolean grid) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("ScenesList", grid).apply();
    }

    public static boolean isPortsUnlimited() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.ports_unlimited);
        return prefs.getBoolean("ports_unlimited", value);
    }

    public static boolean logEnergySaveMode() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.log_energy_saving_mode);
        return prefs.getBoolean("use_log_energy_saving_mode", value);
    }

    public static boolean isPreferenceNameLogEnergySaveMode(String name) {
        return name.equals("use_log_energy_saving_mode");
    }

    /**
     * @return Return true if first run. All later calls will always return false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFirstRun() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = prefs.getBoolean("firstRun", true);
        prefs.edit().putBoolean("firstRun", false).apply();
        return value;
    }

    public static boolean gDriveEnabled() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("backup_to_gDrive", false);
    }

    public static void saveNeighbours(String json) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("neighbours", json).apply();
    }

    public static String loadNeighbours() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("neighbours", null);
    }

    public static boolean isNotification() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_persistent_notification);
        return prefs.getBoolean(PREF_show_persistent_notification, value);
    }

    public static boolean isHideNotReachable() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.hide_not_reachable);
        return prefs.getBoolean(hide_not_reachable, value);
    }

    public static boolean notifyDeviceNotReachable() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.notify_on_non_reachable);
        return prefs.getBoolean("notify_on_non_reachable", value);
    }
}
