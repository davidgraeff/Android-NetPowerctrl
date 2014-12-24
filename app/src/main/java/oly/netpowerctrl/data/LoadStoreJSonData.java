package oly.netpowerctrl.data;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import oly.netpowerctrl.device_base.data.StorableInterface;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.ui.notifications.InAppNotifications;


/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread. WIP
 */
public class LoadStoreJSonData implements onStorageUpdate {

    @Override
    public void save(CollectionWithType collection, StorableInterface item) {
        if (item.getStorableName() == null)
            throw new RuntimeException("Save failed, name is null: " + item.getClass().getCanonicalName());

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
        if (children != null)
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
        appData.deviceCollection.setStorage(this);
        appData.sceneCollection.setStorage(this);
        appData.groupCollection.setStorage(this);
        appData.timerCollection.setStorage(this);
        appData.favCollection.setStorage(this);

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                Thread.currentThread().setName("loadDataThread");
                readOtherThread(appData);

                return null;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                SharedPrefs.getInstance().setCurrentPreferenceVersion();
                AppData.setDataLoadingCompleted();
            }
        }.execute();
    }

    public void finish(AppData appData) {
        appData.deviceCollection.setStorage(null);
        appData.sceneCollection.setStorage(null);
        appData.groupCollection.setStorage(null);
        appData.timerCollection.setStorage(null);
        appData.favCollection.setStorage(null);
    }

    private <COLLECTION, ITEM extends StorableInterface>
    void readOtherThreadCollection(CollectionWithStorableItems<COLLECTION, ITEM> collection,
                                   Class<ITEM> classType, boolean keepOnFailure) throws IllegalAccessException, InstantiationException {

        File fileDir = new File(App.instance.getFilesDir(), collection.type());

        collection.getItems().clear();
        File[] files = fileDir.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            ITEM item = classType.newInstance();
            try {
                item.load(new FileInputStream(file));
                collection.addWithourSave(item);
            } catch (IOException e) {
                e.printStackTrace();
                failedToRead(e, file, item);
            } catch (ClassNotFoundException e) {
                if (keepOnFailure)
                    e.printStackTrace();
                else
                    failedToRead(e, file, item);
            }
        }
    }

    private <ITEM> void failedToRead(Exception e, File file, ITEM item) {
        try {
            byte fileContent[] = new byte[(int) file.length()];
            new FileInputStream(file).read(fileContent);
            InAppNotifications.silentException(e, new String(fileContent));
        } catch (IOException ignored) {
            InAppNotifications.silentException(e, null);
        }

        InAppNotifications.FromOtherThread(App.instance, "Failed to read " + item.getClass().getSimpleName());
        if (!file.delete())
            Log.e("readOtherThreadCollection", "Failed to delete " + file.getAbsolutePath());
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
            readOtherThreadCollection(appData.timerCollection, Timer.class, false);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(appData.favCollection, FavCollection.FavItem.class, false);
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
