package oly.netpowerctrl.data;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import oly.netpowerctrl.data.storage_container.CollectionStorage;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.ExecutableFabric;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.ioconnection.IOConnectionFabric;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.status_bar.FavItem;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.FactoryInterface;
import oly.netpowerctrl.utils.IOInterface;


/**
 * For loading and storing scenes, groups, devices to local storage, google drive, neighbours.
 * Storing data is done in a thread. WIP
 */
public class LoadStoreCollections implements onStorageUpdate {

    @Override
    public void save(CollectionStorage collection, IOInterface item) {
        if (item.getUid() == null)
            throw new RuntimeException("Save failed, name is null: " + item.getClass().getCanonicalName());

        File dir = new File(App.instance.getFilesDir(), collection.type());
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, item.getUid());
        try {
            FileOutputStream f = new FileOutputStream(file);
            item.save(f);
            f.flush();
            f.close();
            if (file.length() == 0)
                throw new IOException("Storing failed " + file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(CollectionStorage collection, IOInterface item) {
        File file = new File(new File(App.instance.getFilesDir(), collection.type()), item.getUid());

        if (!file.delete())
            Log.e("LoadStoreCollections", "Failed to delete " + file.getAbsolutePath());
    }

    @Override
    public void clear(CollectionStorage collection) {
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
     * @param dataService Notify all observers of the RuntimeDataControllerState that we
     *                    reloaded data. This should invalidate all caches (icons etc).
     */
    public void loadData(final DataService dataService) {
        dataService.credentials.getStorage().setStorage(this);
        dataService.connections.getStorage().setStorage(this);
        dataService.executables.getStorage().setStorage(this);
        dataService.groups.getStorage().setStorage(this);
        dataService.timers.getStorage().setStorage(this);
        dataService.favourites.getStorage().setStorage(this);

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                Thread.currentThread().setName("loadDataThread");
                readOtherThread(dataService);

                return null;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                SharedPrefs.getInstance().setCurrentPreferenceVersion();
                DataService.setDataLoadingCompleted();
            }
        }.execute();
    }

    public void finish(DataService dataService) {
        dataService.credentials.getStorage().setStorage(null);
        dataService.connections.getStorage().setStorage(null);
        dataService.executables.getStorage().setStorage(null);
        dataService.groups.getStorage().setStorage(null);
        dataService.timers.getStorage().setStorage(null);
        dataService.favourites.getStorage().setStorage(null);
    }

    private <ITEM extends IOInterface>
    void readOtherThreadCollection(CollectionStorage<ITEM> collection,
                                   Class<ITEM> classType, boolean keepOnFailure) throws IllegalAccessException, InstantiationException {

        File fileDir = new File(App.instance.getFilesDir(), collection.type());

        collection.clear();
        File[] files = fileDir.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (file.length() == 0) {
                if (!file.delete())
                    throw new RuntimeException();
                continue;
            }

            ITEM item = classType.newInstance();
            try {
                item.load(new FileInputStream(file));
                item.resetChanged();
                collection.addWithoutSave(item);
            } catch (IOException e) {
                e.printStackTrace();
                failedToRead(e, file, item);
            } catch (ClassNotFoundException e) {
                if (keepOnFailure) {
                    Log.w("LoadStore", "Failed to read " + file.getName());
                    //e.printStackTrace();
                } else
                    failedToRead(e, file, item);
            }
        }
    }

    private <ITEM extends IOInterface>
    void readOtherThreadCollection(CollectionStorage<ITEM> collection,
                                   FactoryInterface<ITEM> factory, boolean keepOnFailure) throws IllegalAccessException, InstantiationException {

        File fileDir = new File(App.instance.getFilesDir(), collection.type());

        collection.clear();
        File[] files = fileDir.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (file.length() == 0) {
                if (!file.delete())
                    throw new RuntimeException();
                continue;
            }

            try {
                ITEM item = factory.newInstance(new FileInputStream(file));
                item.resetChanged();
                collection.addWithoutSave(item);
            } catch (IOException e) {
                //e.printStackTrace();
                failedToRead(e, file, null);
            } catch (ClassNotFoundException e) {
                if (keepOnFailure)
                    e.printStackTrace();
                else
                    failedToRead(e, file, null);
            }
        }
    }

    private <ITEM> void failedToRead(Exception e, File file, @Nullable ITEM item) {
        try {
            byte fileContent[] = new byte[(int) file.length()];
            //noinspection ResultOfMethodCallIgnored
            new FileInputStream(file).read(fileContent);
            InAppNotifications.silentException(e, new String(fileContent));
        } catch (IOException ignored) {
            InAppNotifications.silentException(e, "Failed to read " + ((item == null) ? file.getName() : item.getClass().getSimpleName()));
        }
        Log.e("LoadStoreCollections", "Failed to read " + ((item == null) ? file.getName() : item.getClass().getSimpleName()));

        if (!file.delete())
            Log.e("LoadStoreCollections", "Failed to delete " + file.getAbsolutePath());
    }

    private void readOtherThread(final DataService dataService) {
        try {
            readOtherThreadCollection(dataService.credentials.getStorage(), Credentials.class, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(dataService.executables.getStorage(), new ExecutableFabric(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(dataService.connections.getStorage(), new IOConnectionFabric(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(dataService.groups.getStorage(), Group.class, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(dataService.timers.getStorage(), Timer.class, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            readOtherThreadCollection(dataService.favourites.getStorage(), FavItem.class, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
