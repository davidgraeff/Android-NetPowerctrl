package oly.netpowerctrl.application_state;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.GroupCollection;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread.
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

    public SceneCollection readScenes() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            return SceneCollection.fromJSON(JSONHelper.getReader(prefs.getString("scenes", null)), sceneCollectionStorage);
        } catch (IOException ignored) {
            Toast.makeText(context, R.string.error_reading_scenes, Toast.LENGTH_SHORT).show();
        }

        return new SceneCollection(sceneCollectionStorage);
    }

    public DeviceCollection readDevices() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            return DeviceCollection.fromJSON(JSONHelper.getReader(prefs.getString("devices", null)), deviceCollectionStorage);
        } catch (Exception ignored) {
            Toast.makeText(context, R.string.error_reading_devices, Toast.LENGTH_SHORT).show();
            return new DeviceCollection(deviceCollectionStorage);
        }
    }

    public GroupCollection readGroups() {
        Context context = NetpowerctrlApplication.instance;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return GroupCollection.fromJSON(JSONHelper.getReader(prefs.getString("groups", null)), groupCollectionStorage);
        } catch (IOException ignored) {
            Toast.makeText(context, R.string.error_reading_groups, Toast.LENGTH_SHORT).show();
            return new GroupCollection(groupCollectionStorage);
        }
    }
}
