package oly.netpowerctrl.preferences;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;

/**
 * Access to preferences stored in the android preference manager. An object of this class
 * also observe some of the preferences and provide listener/observer pattern.
 * This class is a singleton (Initialization on Demand Holder).
 */
public class SharedPrefs {
    public final static String PREF_hide_not_reachable = "hide_not_reachable";
    public final static String PREF_use_dark_theme = "use_dark_theme";
    public final static String PREF_background = "show_background";
    public final static String PREF_fullscreen = "fullscreen";
    public final static String PREF_show_persistent_notification = "show_persistent_notification";
    public final static String PREF_default_fallback_icon_set = "default_fallback_icon_set";
    public final static String PREF_last_group_uid = "last_group_uid";
    public final static String PREF_OutletsViewType = "OutletsViewType";
    public final static String PREF_LastScrollIndex = "LastScrollIndex";

    private final static int PREF_CURRENT_VERSION = 4;
    private final Context context;

    private SharedPrefs() {
        this.context = App.instance;
        setBackupPassword(getBackupPassword());
    }

    public static SharedPrefs getInstance() {
        return SingletonHolder.instance;
    }

    public static String getVersionName(Context context) {
        try {
            Class cls = App.class;
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    comp.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static int getVersionCode(Context context) {
        try {
            //noinspection ConstantConditions
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getDefaultFallbackIconSet(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = context.getResources().getString(R.string.default_fallback_icon_set);
        return prefs.getString(PREF_default_fallback_icon_set, value);
    }

    public static long getNextAlarmCheckTimestamp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("next_alarm_timestamp", -1);
    }

    public static String getNextAlarmName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("next_alarm_name", "");
    }

    public static void setNextAlarmCheckTimestamp(Context context, long next_alarm_timestamp, String targetName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("next_alarm_timestamp", next_alarm_timestamp).putString("next_alarm_name", targetName).apply();
    }

    public static boolean isNotification(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_persistent_notification);
        return prefs.getBoolean(PREF_show_persistent_notification, value);
    }

    public static String getAndroidID() {
        return Settings.Secure.getString(App.instance.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public void setDefaultFallbackIconSet(String new_theme) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_default_fallback_icon_set, new_theme).apply();
    }

    public void setCurrentPreferenceVersion() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("prefVersion", PREF_CURRENT_VERSION).apply();
    }

    public void saveWidget(int widgetID, String executableUID, String widgetType) {
        final String prefName = widgetType + String.valueOf(widgetID);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().putString("UUID", executableUID).apply();
    }

    public void deleteWidget(int appWidgetId, String widgetType) {
        final String prefName = widgetType + String.valueOf(appWidgetId);
        SharedPreferences device_prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        device_prefs.edit().clear().apply();
    }

    public String loadWidget(int widgetID, String widgetType) {
        final String prefName = widgetType + String.valueOf(widgetID);
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

    public boolean isMaximumEnergySaving() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean wakeUp_energy_saving_mode = context.getResources().getBoolean(R.bool.maximum_energy_saving_mode);
        try {
            wakeUp_energy_saving_mode = prefs.getBoolean("maximum_energy_saving_mode", wakeUp_energy_saving_mode);
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

    public int getOutletsViewType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_OutletsViewType, 0);
    }

    public void setOutletsViewType(int type) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_OutletsViewType, type).apply();
    }

    public int getLastScrollIndex() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_LastScrollIndex, 0);
    }

    public void setLastScrollIndex(int type) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_LastScrollIndex, type).apply();
    }

    public boolean logEnergy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_energy", false);
    }

    public boolean logExtensions() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_extensions", false);
    }

    public boolean logAlarm() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_alarm", false);
    }

    public boolean logWidget() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_widgets", false);
    }

    public boolean logDetection() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_detect", false);
    }

    public boolean isPreferenceNameLogs(String name) {
        return name.equals("use_log_energy") || name.equals("use_log_extensions") || name.equals("use_log_alarm") || name.equals("use_log_widgets") || name.equals("use_log_detect");
    }

    /**
     * @return Return true if this version is newer than a previous stored version. A
     * version is stored by calling {@see acceptUpdatedVersion}. False is returned if
     * this app runs the first time and for all subsequent starts with the same version.
     */
    public boolean hasBeenUpdated() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastStoredVersion = prefs.getString("lastVersion", null);
        String currentVersion = getVersionName(context);
        if (lastStoredVersion == null) {
            prefs.edit().putString("lastVersion", currentVersion).apply();
            return false;
        }
        return !currentVersion.equals(lastStoredVersion);
    }

    public void acceptUpdatedVersion() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentVersion = getVersionName(context);
        prefs.edit().putString("lastVersion", currentVersion).apply();
    }

    public boolean isHideNotReachable() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.hide_not_reachable);
        return prefs.getBoolean(PREF_hide_not_reachable, value);
    }

    public boolean isBackground() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.show_background);
        return prefs.getBoolean(PREF_background, value);
    }

    public boolean isFullscreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.fullscreen);
        return prefs.getBoolean(PREF_fullscreen, value);
    }

    public long getLastTimeOpenIssuesRequested() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("open_issues_last_access", -1);
    }

    public long getLastTimeOpenAutoIssuesRequested() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("open_auto_issues_last_access", -1);
    }

    public boolean getSmallerClickExecuteArea() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.smaller_click_execute_area);
        return prefs.getBoolean("smaller_click_execute_area", value);
    }

    public String getBackupPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("backup_password", getAndroidID());
    }

    public void setBackupPassword(String password) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("backup_password", password).apply();
    }

    public String getLastGroupUid() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_last_group_uid, null);
    }

    public void setLastGroupUID(String groupUID) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_last_group_uid, groupUID).apply();
    }

    public void setOpenIssues(long last_access) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("open_issues_last_access", last_access).apply();
    }

    public void setOpenAutoIssues(long last_access) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("open_auto_issues_last_access", last_access).apply();
    }

    public boolean isFirstTimeSceneAdd() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean first = prefs.getBoolean("isFirstTimeSceneAdd2", true);
        prefs.edit().putBoolean("isFirstTimeSceneAdd2", false).apply();
        return first;
    }

    public boolean isVoted(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("vote_" + name, false);
    }

    public void vote(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("vote_" + name, true).apply();
    }

    private static class SingletonHolder {
        public static final SharedPrefs instance = new SharedPrefs();
    }
}
