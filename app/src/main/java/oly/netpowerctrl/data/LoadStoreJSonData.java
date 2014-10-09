package oly.netpowerctrl.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.utils.notifications.InAppNotifications;


/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread. WIP
 */
public class LoadStoreJSonData implements onStorageUpdate {

    @Override
    public void save(CollectionWithType collection, StorableInterface item) {
        File dir = new File(App.instance.getFilesDir(), collection.type());
        dir.mkdirs();
        File file = new File(dir, item.getStorableName());
        try {
            FileOutputStream f = new FileOutputStream(file);
            item.save(f);
            f.flush();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(CollectionWithType collection, StorableInterface item) {
        File file = new File(new File(App.instance.getFilesDir(), collection.type()), item.getStorableName());
        file.delete();
    }

    @Override
    public void clear(CollectionWithType collection) {
        File dir = new File(App.instance.getFilesDir(), collection.type());
        String[] children = dir.list();
        for (String aChildren : children) {
            //noinspection ResultOfMethodCallIgnored
            new File(dir, aChildren).delete();
        }
    }

    /**
     * Call this to reload all data from disk. This is useful after NFC/Neighbour/GDrive sync.
     *
     * @param appData Notify all observers of the RuntimeDataControllerState that we
     *                reloaded data. This should invalidate all caches (icons etc).
     */
    public void loadData(final AppData appData) {
        final int lastPrefVersion = SharedPrefs.getInstance().getLastPreferenceVersion();
        appData.deviceCollection.setStorage(this);
        appData.sceneCollection.setStorage(this);
        appData.groupCollection.setStorage(this);
        appData.timerController.setStorage(this);

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                Thread.currentThread().setName("loadDataThread");
                readOtherThread(appData);
                // LEGACY import
                if (lastPrefVersion == 3) {
                    try {
                        LEGACY_READ_FROM_PREFERENCES a = new LEGACY_READ_FROM_PREFERENCES();
                        a.read(appData);
                    } catch (Exception e) {
                        InAppNotifications.FromOtherThread(App.instance, R.string.error_import_legacy_data);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                SharedPrefs.getInstance().setCurrentPreferenceVersion();
                AppData.observersOnDataLoaded.dataLoaded = true;
                AppData.observersOnDataLoaded.onDataLoaded();
            }
        }.execute();
    }

    public void finish() {
        AppData appData = AppData.getInstance();
        appData.deviceCollection.setStorage(null);
        appData.sceneCollection.setStorage(null);
        appData.groupCollection.setStorage(null);
        appData.timerController.setStorage(null);
    }

    private <COLLECTION, ITEM extends StorableInterface>
    void readOtherThreadCollection(CollectionWithStorableItems<COLLECTION, ITEM> collection,
                                   Class<ITEM> classType, boolean keepOnFailure) throws IllegalAccessException, InstantiationException {

        File files;

        { // legacy folder support
            files = App.instance.getDir(collection.type(), 0);
            if (files.exists())
                //noinspection ResultOfMethodCallIgnored
                files.renameTo(new File(App.instance.getFilesDir(), collection.type()));
        }

        files = new File(App.instance.getFilesDir(), collection.type());

        collection.getItems().clear();
        for (File file : files.listFiles()) {
            ITEM item = classType.newInstance();
            try {
                item.load(new FileInputStream(file));
                collection.getItems().add(item);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                if (keepOnFailure)
                    e.printStackTrace();
                else if (!file.delete())
                    Log.e("readOtherThreadCollection", "Failed to delete " + file.getAbsolutePath());
            }
        }
    }

    private void readOtherThread(final AppData appData) {
        try {
            readOtherThreadCollection(appData.deviceCollection, Device.class, true);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(appData.sceneCollection, Scene.class, true);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(appData.groupCollection, Group.class, true);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(appData.timerController, Timer.class, false);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    static public class LEGACY_READ_FROM_PREFERENCES {
        private final Context context;

        public LEGACY_READ_FROM_PREFERENCES() {
            this.context = App.instance;
        }

        /**
         * @param reader A json reader
         * @throws IOException
         * @throws IllegalStateException
         */
        public <ITEM extends StorableInterface> void fromJSON(List<ITEM> items, JsonReader reader, Class<ITEM> classType)
                throws IOException, IllegalStateException, IllegalAccessException, InstantiationException {

            if (reader == null)
                return;

            reader.beginArray();
            while (reader.hasNext()) {
                try {
                    ITEM item = classType.newInstance();
                    item.load(reader);
                    items.add(item);
                } catch (ClassNotFoundException e) {
                    // If we read a device description, where we do not support that device type,
                    // we just ignore that device and go on. Nevertheless print a backtrace.
                    e.printStackTrace();
                }
            }
            reader.endArray();
        }

        public void read(final AppData appData) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            try {
                fromJSON(appData.sceneCollection.getItems(), JSONHelper.getReader(prefs.getString("scenes", null)), Scene.class);
                if (appData.sceneCollection.getItems().size() > 0)
                    appData.sceneCollection.saveAll();
            } catch (IOException | InstantiationException | IllegalAccessException ignored) {
                InAppNotifications.FromOtherThread(context, R.string.error_reading_scenes);
            }
            try {
                fromJSON(appData.deviceCollection.getItems(), JSONHelper.getReader(prefs.getString("devices", null)), Device.class);
                if (appData.deviceCollection.getItems().size() > 0)
                    appData.deviceCollection.saveAll();

            } catch (IOException | InstantiationException | IllegalAccessException ignored) {
                InAppNotifications.FromOtherThread(context, R.string.error_reading_devices);
            }
            try {
                fromJSON(appData.groupCollection.getItems(), JSONHelper.getReader(prefs.getString("groups", null)), Group.class);
                if (appData.groupCollection.getItems().size() > 0)
                    appData.groupCollection.saveAll();

            } catch (IOException | InstantiationException | IllegalAccessException ignored) {
                InAppNotifications.FromOtherThread(context, R.string.error_reading_groups);
            }
            try {
                fromJSON(appData.timerController.getItems(), JSONHelper.getReader(prefs.getString("alarms", null)), Timer.class);
                if (appData.timerController.getItems().size() > 0)
                    appData.timerController.saveAll();
            } catch (IOException | InstantiationException | IllegalAccessException ignored) {
                InAppNotifications.FromOtherThread(context, R.string.error_reading_alarms);
            }
        }
    }
}
