package oly.netpowerctrl.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.JsonReader;

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
import oly.netpowerctrl.utils.ShowToast;

/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread. WIP
 */
public class LoadStoreJSonData {

    private final onStorageUpdate storageUpdate = new onStorageUpdate() {
        @Override
        public void save(CollectionWithType collection, Storable item) {
            File file = new File(App.instance.getDir(collection.type(), 0), item.getStorableName());
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
        public void remove(CollectionWithType collection, Storable item) {
            File file = new File(App.instance.getDir(collection.type(), 0), item.getStorableName());
            file.delete();
        }

        @Override
        public void clear(CollectionWithType collection) {
            File dir = App.instance.getDir(collection.type(), 0);
            String[] children = dir.list();
            for (String aChildren : children) {
                //noinspection ResultOfMethodCallIgnored
                new File(dir, aChildren).delete();
            }
        }
    };

    /**
     * Call this to reload all data from disk. This is useful after NFC/Neighbour/GDrive sync.
     *
     * @param appData Notify all observers of the RuntimeDataControllerState that we
     *                reloaded data. This should invalidate all caches (icons etc).
     */
    public void loadData(final AppData appData) {
        final int lastPrefVersion = SharedPrefs.getInstance().getLastPreferenceVersion();
        appData.deviceCollection.setStorage(storageUpdate);
        appData.sceneCollection.setStorage(storageUpdate);
        appData.groupCollection.setStorage(storageUpdate);
        appData.timerController.setStorage(storageUpdate);

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
                    LEGACY_READ_FROM_PREFERENCES a = new LEGACY_READ_FROM_PREFERENCES();
                    a.read(appData);
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

    private void readOtherThread(final AppData appData) {
        appData.deviceCollection.getItems().clear();
        appData.sceneCollection.getItems().clear();
        appData.groupCollection.getItems().clear();
        appData.timerController.getItems().clear();

        File files;

        files = App.instance.getDir(appData.deviceCollection.type(), 0);
        for (File file : files.listFiles()) {
            Device item = new Device("");
            try {
                item.load(new FileInputStream(file));
                appData.deviceCollection.items.add(item);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        files = App.instance.getDir(appData.sceneCollection.type(), 0);
        for (File file : files.listFiles()) {
            Scene item = new Scene();
            try {
                item.load(new FileInputStream(file));
                appData.sceneCollection.items.add(item);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        files = App.instance.getDir(appData.groupCollection.type(), 0);
        for (File file : files.listFiles()) {
            Group item = new Group(null, null);
            try {
                item.load(new FileInputStream(file));
                appData.groupCollection.items.add(item);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        files = App.instance.getDir(appData.timerController.type(), 0);
        for (File file : files.listFiles()) {
            Timer item = new Timer();
            try {
                item.load(new FileInputStream(file));
                appData.timerController.items.add(item);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
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
        public <T extends Storable> void fromJSON(List<T> items, JsonReader reader, CreateNewObject<T> creator)
                throws IOException, IllegalStateException {

            if (reader == null)
                return;

            reader.beginArray();
            while (reader.hasNext()) {
                try {
                    T item = creator.create();
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
                fromJSON(appData.sceneCollection.getItems(), JSONHelper.getReader(prefs.getString("scenes", null)), new CreateNewObject<Scene>() {
                    @Override
                    public Scene create() {
                        return new Scene();
                    }
                });
                if (appData.sceneCollection.getItems().size() > 0)
                    appData.sceneCollection.saveAll();
            } catch (IOException ignored) {
                ShowToast.FromOtherThread(context, R.string.error_reading_scenes);
            }
            try {
                fromJSON(appData.deviceCollection.getItems(), JSONHelper.getReader(prefs.getString("devices", null)), new CreateNewObject<Device>() {
                    @Override
                    public Device create() {
                        return new Device(null);
                    }
                });
                if (appData.deviceCollection.getItems().size() > 0)
                    appData.deviceCollection.saveAll();

            } catch (Exception ignored) {
                ShowToast.FromOtherThread(context, R.string.error_reading_devices);
            }
            try {
                fromJSON(appData.groupCollection.getItems(), JSONHelper.getReader(prefs.getString("groups", null)), new CreateNewObject<Group>() {
                    @Override
                    public Group create() {
                        return new Group(null, null);
                    }
                });
                if (appData.groupCollection.getItems().size() > 0)
                    appData.groupCollection.saveAll();

            } catch (IOException ignored) {
                ShowToast.FromOtherThread(context, R.string.error_reading_groups);
            }
            try {
                fromJSON(appData.timerController.getItems(), JSONHelper.getReader(prefs.getString("alarms", null)), new CreateNewObject<Timer>() {
                    @Override
                    public Timer create() {
                        return new Timer();
                    }
                });
                if (appData.timerController.getItems().size() > 0)
                    appData.timerController.saveAll();
            } catch (IOException ignored) {
                ShowToast.FromOtherThread(context, R.string.error_reading_alarms);
            }
        }


        private interface CreateNewObject<T> {
            T create();
        }
    }
}
