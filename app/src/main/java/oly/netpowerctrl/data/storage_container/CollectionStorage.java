package oly.netpowerctrl.data.storage_container;

import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.utils.IOInterface;

/**
 * Created by david on 27.04.15.
 */
public abstract class CollectionStorage<ITEM extends IOInterface> {
    protected onStorageUpdate storage;

//    public void saveAll() {
//        if (storage == null)
//            return;
//
//        storage.clear(this);
//        for (ITEM item : items.values())
//            storage.save(this, item);
//    }

    public void save(ITEM item) {
        if (storage != null)
            storage.save(this, item);
        item.resetChanged();
    }

    public void remove(ITEM item) {
        if (storage != null)
            storage.remove(this, item);
    }

    final public void setStorage(onStorageUpdate storage) {
        this.storage = storage;
    }

    abstract public void clear();

    abstract public void addWithoutSave(ITEM item) throws ClassNotFoundException;

    abstract public String type();
}
