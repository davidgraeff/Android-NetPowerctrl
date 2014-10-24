package oly.netpowerctrl.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.WeakHashMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;

/**
 * Access to preferences stored in the android preference manager. An object of this class
 * also observe some of the preferences and provide listener/observer pattern.
 * This class is a singleton (Initialization on Demand Holder).
 */
public class SharedPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static String PREF_widgets = "widgets";
    public final static String PREF_WIDGET_BASENAME = "oly.netpowerctrl.widget";
    public final static String hide_not_reachable = "hide_not_reachable";
    public final static String PREF_use_dark_theme = "use_dark_theme";
    public final static String PREF_background = "show_background";
    public final static String PREF_fullscreen = "fullscreen";
    public final static String PREF_show_persistent_notification = "show_persistent_notification";
    public final static String PREF_default_fallback_icon_set = "default_fallback_icon_set";

    private final static int PREF_CURRENT_VERSION = 4;
    private final static String firstTabExtraFilename = "firstTabExtra";
    private final Context context;
    private final WeakHashMap<IHideNotReachable, Boolean> observers_HideNotReachable = new WeakHashMap<>();
    private final WeakHashMap<IShowPersistentNotification, Boolean> observers_ShowPersistentNotification = new WeakHashMap<>();

    private SharedPrefs() {
        this.context = App.instance;
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
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
        try {
            return prefs.getString("FIRST_TAB", "");
        } catch (Exception ignored) {
        }
        return "";
    }

    public int getFirstTabPosition() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return prefs.getInt("FIRST_TAB_POSITION", -1);
        } catch (Exception ignored) {
        }
        return -1;
    }

    public Bundle getFirstTabExtra() {
        File file = context.getFileStreamPath(firstTabExtraFilename);
        if (!file.exists())
            return null;

        Parcel parcel = Parcel.obtain(); //creating empty parcel object
        try {
            FileInputStream fis = context.openFileInput(firstTabExtraFilename);
            byte[] array = new byte[(int) fis.getChannel().size()];
            //noinspection ResultOfMethodCallIgnored
            fis.read(array, 0, array.length);
            fis.close();
            parcel.unmarshall(array, 0, array.length);
            parcel.setDataPosition(0);
            return parcel.readBundle();
            //out.putAll(out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
        }
        return null;
    }

    public void setFirstTab(String fragmentClassName, Bundle extra, int position) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString("FIRST_TAB", fragmentClassName);
        prefEditor.putInt("FIRST_TAB_POSITION", position);
        prefEditor.apply();
        if (extra != null) {
            Parcel parcel = Parcel.obtain(); //creating empty parcel object
            try {
                FileOutputStream fos = context.openFileOutput(firstTabExtraFilename, Context.MODE_PRIVATE);
                extra.writeToParcel(parcel, 0); //saving bundle as parcel
                fos.write(parcel.marshall()); //writing parcel to file
                fos.flush();
                fos.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                parcel.recycle();
            }
        } else {
            context.deleteFile(firstTabExtraFilename);
        }
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

    public int getOutletsViewType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("OutletsViewType", 0);
    }

    public void setOutletsViewType(int type) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("OutletsViewType", type).apply();
    }

    public boolean logEnergySaveMode() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_energy_saving_mode", false);
    }

    public boolean logExtensions() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_log_extensions", false);
    }

    public boolean isPreferenceNameLogEnergySaveMode(String name) {
        return name.equals("use_log_energy_saving_mode");
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

    public boolean isOutletEditingEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.outlet_editing_enabled);
        return prefs.getBoolean("outlet_editing_enabled", value);
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

    public int getOpenIssues() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("open_issues", -1);
    }

    public long getLastTimeOpenIssuesRequested() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("open_issues_last_access", -1);
    }

    public boolean getSmallerClickExecuteArea() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean value = context.getResources().getBoolean(R.bool.smaller_click_execute_area);
        return prefs.getBoolean("smaller_click_execute_area", value);
    }

    public String getBackupPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return prefs.getString("backup_password", value);
    }

    public void setBackupPassword(String password) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("backup_password", password).apply();
    }

    public void setOpenIssues(int value, long last_access) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt("open_issues", value).putLong("open_issues_last_access", last_access).apply();
    }

    public void registerHideNotReachable(IHideNotReachable observer) {
        observers_HideNotReachable.put(observer, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        boolean value;
        switch (s) {
            case SharedPrefs.hide_not_reachable: {
                value = isHideNotReachable();
                Set<IHideNotReachable> observers = observers_HideNotReachable.keySet();
                for (IHideNotReachable observer : observers) {
                    observer.hideNotReachable(value);
                }
                break;
            }
            case SharedPrefs.PREF_show_persistent_notification: {
                value = isNotification();
                Set<IShowPersistentNotification> observers = observers_ShowPersistentNotification.keySet();
                for (IShowPersistentNotification observer : observers) {
                    observer.showPersistentNotificationChanged(value);
                }
                break;
            }
        }
    }

    public void registerShowPersistentNotification(IShowPersistentNotification observer) {
        observers_ShowPersistentNotification.put(observer, true);
    }

    public interface IShowPersistentNotification {
        void showPersistentNotificationChanged(boolean enabled);
    }

    public interface IShowBackground {
        void backgroundChanged(boolean showBackground);
    }

    public interface IHideNotReachable {
        void hideNotReachable(boolean hideNotReachable);
    }

    private static class SingletonHolder {
        public static final SharedPrefs instance = new SharedPrefs();
    }
}
