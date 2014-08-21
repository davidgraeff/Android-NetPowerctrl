package oly.netpowerctrl.preferences;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;

import java.util.Set;
import java.util.WeakHashMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class SharedPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static String PREF_widgets = "widgets";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public static final String PREF_use_dark_theme = "use_dark_theme";
    public static final String PREF_show_persistent_notification = "show_persistent_notification";
    public final static String hide_not_reachable = "hide_not_reachable";
    private final static int PREF_CURRENT_VERSION = 3;
    private static SharedPrefs instance;
    private WeakHashMap<IShowBackground, Boolean> observers_showBackground = new WeakHashMap<>();
    private WeakHashMap<IHideNotReachable, Boolean> observers_HideNotReachable = new WeakHashMap<>();

    public SharedPrefs(Application app) {
        instance = this;
        PreferenceManager.getDefaultSharedPreferences(app).unregisterOnSharedPreferenceChangeListener(this);
    }

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

    public static String getVersionName(Context context) {
        try {
            Class cls = NetpowerctrlApplication.class;
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                    comp.getPackageName(), 0);
            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * @return Return true if first run. All later calls will always return false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean showChangeLog() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastStoredVersion = prefs.getString("lastVersion", null);
        String currentVersion = getVersionName(context);
        // Do not show changelog on first app opening (but on the next opening)
        if (lastStoredVersion == null && getFirstTab().equals("")) {
            prefs.edit().putString("lastVersion", "").apply();
            return false;
        }
        // last version == current version: do not show changelog
        if (currentVersion.equals(lastStoredVersion))
            return false;
        // update last version and show change log
        prefs.edit().putString("lastVersion", currentVersion).apply();
        return true;
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

    public static boolean isBackground() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_background);
        return prefs.getBoolean("show_background", value);
    }

    public static int getOpenIssues() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("open_issues", -1);
    }

    public static long getLastTimeOpenIssuesRequested() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("open_issues_last_access", -1);
    }

    public static void setOpenIssues(int value, long last_access) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("open_issues", value).putLong("open_issues_last_access", last_access).apply();
    }

    public static SharedPrefs getInstance() {
        return instance;
    }

    public void registerShowBackground(IShowBackground observer) {
        observers_showBackground.put(observer, true);
    }

    public void registerHideNotReachable(IHideNotReachable observer) {
        observers_HideNotReachable.put(observer, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("show_background")) {

        }
        boolean value;
        switch (s) {
            case "show_background": {
                value = isBackground();
                Set<IShowBackground> observers = observers_showBackground.keySet();
                for (IShowBackground observer : observers) {
                    observer.backgroundChanged(value);
                }
                break;
            }
            case SharedPrefs.hide_not_reachable: {
                value = isHideNotReachable();
                Set<IHideNotReachable> observers = observers_HideNotReachable.keySet();
                for (IHideNotReachable observer : observers) {
                    observer.hideNotReachable(value);
                }
                break;
            }
        }
    }

    public interface IShowBackground {
        void backgroundChanged(boolean showBackground);
    }

    public interface IHideNotReachable {
        void hideNotReachable(boolean hideNotReachable);
    }
}
