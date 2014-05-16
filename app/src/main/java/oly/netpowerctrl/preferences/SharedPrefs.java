package oly.netpowerctrl.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class SharedPrefs {
    public final static String PREF_widgets = "widgets";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
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
        prefs.edit().putInt("prefVersion", PREF_CURRENT_VERSION).commit();
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
        prefEditor.commit();
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
        prefs.edit().putBoolean("showHiddenOutlets", showHiddenOutlets).commit();
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
        device_prefs.edit().putString("UUID", devicePortUuid).commit();
    }

    public static void DeleteWidgets(int appWidgetId) {
        Context context = NetpowerctrlApplication.instance;
        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().commit();
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

    public static boolean getUse_energy_saving_mode() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean use_energy_saving_mode = context.getResources().getBoolean(R.bool.use_energy_saving_mode);
        try {
            use_energy_saving_mode = prefs.getBoolean("use_energy_saving_mode", use_energy_saving_mode);
        } catch (Exception ignored) {
        }
        return use_energy_saving_mode;
    }

    public static boolean getWakeUp_energy_saving_mode() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wakeUp_energy_saving_mode = context.getResources().getBoolean(R.bool.wakeup_energy_saving_mode);
        try {
            wakeUp_energy_saving_mode = prefs.getBoolean("wakeup_energy_saving_mode", wakeUp_energy_saving_mode);
        } catch (Exception ignored) {
        }
        return wakeUp_energy_saving_mode;
    }

    public static final String PREF_use_dark_theme = "use_dark_theme";

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

    public static boolean getScenesList() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("ScenesList", false);
    }

    public static void setOutletsGrid(boolean grid) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("OutletsGrid", grid).commit();
    }

    public static void setScenesList(boolean grid) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("ScenesList", grid).commit();
    }

    public static boolean getAnimationEnabled() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.animations_enabled);
        return prefs.getBoolean("animations_enabled", value);
    }

    public static boolean logEnergySaveMode() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.use_log_energy_saving_mod);
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
        prefs.edit().putBoolean("firstRun", false).commit();
        return value;
    }

    public static boolean gDriveEnabled() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("gDrive", false);
    }

    public static void setGDriveEnabled(boolean enabled) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("gDrive", enabled).commit();
    }

    public static void saveNeighbours(String json) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("neighbours", json).commit();
    }

    public static String loadNeighbours() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("neighbours", null);
    }

    public static boolean isNeighbourAutoSync() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("neighbour_sync", false);
    }

    public static void setNeighbourAutoSync(boolean enabled) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("neighbour_sync", enabled).commit();
    }
}
