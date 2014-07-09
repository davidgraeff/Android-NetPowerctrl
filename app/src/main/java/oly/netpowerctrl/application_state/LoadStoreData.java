package oly.netpowerctrl.application_state;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.alarms.TimerController;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread. WIP
 */
public class LoadStoreData {
    private final static String GROUPS_DIR = "groups";
    private final static String DEVICES_DIR = "devices";
    private final static String SCENES_DIR = "scenes";


    private final SceneCollection.IScenesSave sceneCollectionStorage = new SceneCollection.IScenesSave() {
        @Override
        public void scenesSave(SceneCollection scenes) {
            Context context = NetpowerctrlApplication.instance;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("scenes", scenes.toJSON()).commit();
        }
    };

    private final GroupCollection.IGroupsSave groupCollectionStorage = new GroupCollection.IGroupsSave() {
        @Override
        public void groupsSave(GroupCollection groups) {
            Context context = NetpowerctrlApplication.instance;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("groups", groups.toJSON()).commit();
        }
    };

    private final DeviceCollection.IDevicesSave deviceCollectionStorage = new DeviceCollection.IDevicesSave() {
        @Override
        public void devicesSave(DeviceCollection devices) {
            Context context = NetpowerctrlApplication.instance;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("devices", devices.toJSON()).commit();
        }
    };

    private final TimerController.IAlarmsSave alarmsStorage = new TimerController.IAlarmsSave() {
        @Override
        public void alarmsSave(TimerController alarms) {
            Context context = NetpowerctrlApplication.instance;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("alarms", alarms.toJSON()).commit();
        }
    };

    public void read(SceneCollection target) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            target.setStorage(sceneCollectionStorage);
            target.fromJSON(JSONHelper.getReader(prefs.getString("scenes", null)), false);
        } catch (IOException ignored) {
            Toast.makeText(context, R.string.error_reading_scenes, Toast.LENGTH_SHORT).show();
        }
    }

    public void read(DeviceCollection target) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            target.setStorage(deviceCollectionStorage);
            target.fromJSON(JSONHelper.getReader(prefs.getString("devices", null)), false);
        } catch (Exception ignored) {
            Toast.makeText(context, R.string.error_reading_devices, Toast.LENGTH_SHORT).show();
        }
    }

    public void read(GroupCollection target) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            target.setStorage(groupCollectionStorage);
            target.fromJSON(JSONHelper.getReader(prefs.getString("groups", null)), false);
        } catch (IOException ignored) {
            Toast.makeText(context, R.string.error_reading_groups, Toast.LENGTH_SHORT).show();
        }
    }

    public void read(TimerController target) {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            target.setStorage(alarmsStorage);
            target.fromJSON(JSONHelper.getReader(prefs.getString("alarms", null)));
        } catch (IOException ignored) {
            Toast.makeText(context, R.string.error_reading_groups, Toast.LENGTH_SHORT).show();
        }
    }

    public void markVersion() {
        SharedPrefs.setCurrentPreferenceVersion();
    }
}
