package oly.netpowerctrl.preferences;

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
    public final static String hide_not_reachable = "hide_not_reachable";
    private final static int PREF_CURRENT_VERSION = 3;
    public static SharedPrefs instance;
    public final String PREF_use_dark_theme = "use_dark_theme";
    public final String PREF_show_persistent_notification = "show_persistent_notification";
    private final Context context;
    private WeakHashMap<IShowBackground, Boolean> observers_showBackground = new WeakHashMap<>();
    private WeakHashMap<IHideNotReachable, Boolean> observers_HideNotReachable = new WeakHashMap<>();

    public SharedPrefs(Context context) {
        instance = this;
        this.context = context;
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
    }

    public static SharedPrefs getInstance() {
        return instance;
    }

    public int getLastPreferenceVersion() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int lastSavedVersion = -1;
        try {
            lastSavedVersion = prefs.getInt("prefVersion", lastSavedVersion);
        } catch (Exception ignored) {
        }
        return lastSavedVersion;
    }

    public void setCurrentPreferenceVersion() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("prefVersion", PREF_CURRENT_VERSION).apply();
    }

    public String getFirstTab() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String tab = "";
        try {
            tab = prefs.getString("FIRST_TAB", "");
        } catch (Exception ignored) {
        }
        return tab;
    }

    public void setFirstTab(String fragmentClassName) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString("FIRST_TAB", fragmentClassName);
        prefEditor.apply();
    }

    public boolean getShowHiddenOutlets() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean ShowHiddenOutlets = context.getResources().getBoolean(R.bool.showHiddenOutlets);
        try {
            ShowHiddenOutlets = prefs.getBoolean("showHiddenOutlets", ShowHiddenOutlets);
        } catch (Exception ignored) {
        }
        return ShowHiddenOutlets;
    }

    public void setShowHiddenOutlets(boolean showHiddenOutlets) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("showHiddenOutlets", showHiddenOutlets).apply();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getLoadExtensions() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean keep_widget_service_running = context.getResources().getBoolean(R.bool.load_extensions);
        try {
            keep_widget_service_running = prefs.getBoolean("load_extensions", false);
        } catch (Exception ignored) {
        }
        return keep_widget_service_running;
    }

    public void SaveWidget(int widgetID, String devicePortUuid) {

        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().putString("UUID", devicePortUuid).apply();
    }

    public void DeleteWidgets(int appWidgetId) {

        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().apply();
    }

    public String LoadWidget(int widgetID) {

        final String prefName = PREF_WIDGET_BASENAME + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return device_prefs.getString("UUID", null);
    }

    public int getDefaultSendPort() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int send_udp = context.getResources().getInteger(R.integer.default_send_port);
        try {
            send_udp = Integer.parseInt(prefs.getString("standard_send_port", Integer.valueOf(send_udp).toString()));
        } catch (Exception ignored) {
        }
        return send_udp;
    }

    public int getDefaultReceivePort() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int receive_port_udp = context.getResources().getInteger(R.integer.default_receive_port);
        try {
            receive_port_udp = Integer.parseInt(prefs.getString("standard_receive_port", Integer.valueOf(receive_port_udp).toString()));
        } catch (Exception ignored) {
        }
        return receive_port_udp;
    }

    public boolean isWakeUpFromEnergySaving() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wakeUp_energy_saving_mode = context.getResources().getBoolean(R.bool.wakeup_energy_saving_mode);
        try {
            wakeUp_energy_saving_mode = prefs.getBoolean("wakeup_energy_saving_mode", wakeUp_energy_saving_mode);
        } catch (Exception ignored) {
        }
        return wakeUp_energy_saving_mode;
    }

    public boolean isDarkTheme() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.use_dark_theme);
        try {
            value = prefs.getBoolean(PREF_use_dark_theme, value);
        } catch (Exception ignored) {
        }
        return value;
    }

    public boolean getOutletsGrid() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("OutletsGrid", false);
    }

    public void setOutletsGrid(boolean grid) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("OutletsGrid", grid).apply();
    }

    public boolean getScenesList() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("ScenesList", false);
    }

    public void setScenesList(boolean grid) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("ScenesList", grid).apply();
    }

    public boolean isPortsUnlimited() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.ports_unlimited);
        return prefs.getBoolean("ports_unlimited", value);
    }

    public boolean logEnergySaveMode() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.log_energy_saving_mode);
        return prefs.getBoolean("use_log_energy_saving_mode", value);
    }

    public boolean isPreferenceNameLogEnergySaveMode(String name) {
        return name.equals("use_log_energy_saving_mode");
    }

    public String getVersionName(Context context) {
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
    public boolean showChangeLog() {

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

    public boolean gDriveEnabled() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("backup_to_gDrive", false);
    }

    public void saveNeighbours(String json) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("neighbours", json).apply();
    }

    public String loadNeighbours() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("neighbours", null);
    }

    public boolean isNotification() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_persistent_notification);
        return prefs.getBoolean(PREF_show_persistent_notification, value);
    }

    public boolean isHideNotReachable() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.hide_not_reachable);
        return prefs.getBoolean(hide_not_reachable, value);
    }

    public boolean notifyDeviceNotReachable() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.notify_on_non_reachable);
        return prefs.getBoolean("notify_on_non_reachable", value);
    }

    public boolean isBackground() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_background);
        return prefs.getBoolean("show_background", value);
    }

    public int getOpenIssues() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("open_issues", -1);
    }

    public long getLastTimeOpenIssuesRequested() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("open_issues_last_access", -1);
    }

    public void setOpenIssues(int value, long last_access) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("open_issues", value).putLong("open_issues_last_access", last_access).apply();
    }

    public void registerShowBackground(IShowBackground observer) {
        observers_showBackground.put(observer, true);
    }

    public void registerHideNotReachable(IHideNotReachable observer) {
        observers_HideNotReachable.put(observer, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
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
