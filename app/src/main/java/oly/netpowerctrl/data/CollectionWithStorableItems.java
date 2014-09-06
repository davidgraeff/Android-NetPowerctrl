package oly.netpowerctrl.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by david on 01.09.14.
 */
abstract public class CollectionWithStorableItems<COLLECTION, ITEM extends StorableInterface> implements CollectionWithType {

    private final WeakHashMap<onCollectionUpdated, Boolean> observers = new WeakHashMap<>();

    protected List<ITEM> items = new ArrayList<>();
    protected onStorageUpdate storage;

    final public List<ITEM> getItems() {
        return items;
    }

    final public void registerObserver(onCollectionUpdated o) {
        observers.put(o, true);
    }

    final public void unregisterObserver(onCollectionUpdated o) {
        observers.remove(o);
    }

    final protected void notifyObservers(ITEM item, ObserverUpdateActions actions) {
        Iterator<onCollectionUpdated> it = observers.keySet().iterator();
        while (it.hasNext())
            if (!it.next().updated(this, item, actions))
                it.remove();
    }

    final public int size() {
        return items.size();
    }

    final public ITEM get(int i) {
        return items.get(i);
    }

    public void saveAll() {
        if (storage == null)
            return;

        storage.clear(this);
        for (StorableInterface item : items)
            storage.save(this, item);
    }

    public onStorageUpdate getStorage() {
        return storage;
    }

    final public void setStorage(onStorageUpdate storage) {
        this.storage = storage;
    }
}
