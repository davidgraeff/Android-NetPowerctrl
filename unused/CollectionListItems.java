package oly.netpowerctrl.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onStorageUpdate;

/**
 * Created by david on 01.09.14.
 */
abstract public class CollectionListItems<COLLECTION, ITEM extends IOInterface> extends CollectionObserver<COLLECTION,ITEM> implements CollectionManipulation<ITEM> {
    final public CollectionStorage<ITEM> storage;
    final public DataService dataService;

    public CollectionListItems(DataService dataService, final String type) {
        this.dataService = dataService;
        storage = new CollectionStorage<ITEM>() {
            @Override
            public void clear() {
                items.clear();
            }

            @Override
            public void addWithoutSave(ITEM item) throws ClassNotFoundException {
                items.add(item);
            }

            @Override
            public String type() {
                return type;
            }
        };
    }

    protected List<ITEM> items = new ArrayList<>();

    final public List<ITEM> getItems() {
        return items;
    }

    final public int size() {
        return items.size();
    }

    final public ITEM get(int i) {
        return items.get(i);
    }

    @Override
    public CollectionStorage<ITEM> getStorage() {
        return storage;
    }
}
