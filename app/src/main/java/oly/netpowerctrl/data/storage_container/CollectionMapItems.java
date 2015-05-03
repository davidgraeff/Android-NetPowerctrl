package oly.netpowerctrl.data.storage_container;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.utils.IOInterface;

/**
 * Created by david on 01.09.14.
 */
abstract public class CollectionMapItems<COLLECTION, ITEM extends IOInterface> extends CollectionObserver<COLLECTION, ITEM> implements CollectionManipulation<ITEM> {
    final public CollectionStorage<ITEM> storage;
    final public DataService dataService;
    protected Map<String, ITEM> items = new TreeMap<>();

    public CollectionMapItems(DataService dataService, final String type) {
        this.dataService = dataService;
        storage = new CollectionStorage<ITEM>() {
            @Override
            public void clear() {
                items.clear();
            }

            @Override
            public void addWithoutSave(ITEM item) throws ClassNotFoundException {
                items.put(item.getUid(), item);
            }

            @Override
            public String type() {
                return type;
            }
        };
    }

    final public Map<String, ITEM> getItems() {
        return items;
    }

    final public int size() {
        return items.size();
    }

    @Override
    public CollectionStorage<ITEM> getStorage() {
        return storage;
    }
}
