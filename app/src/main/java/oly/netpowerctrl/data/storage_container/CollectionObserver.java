package oly.netpowerctrl.data.storage_container;

import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * Created by david on 27.04.15.
 */
public class CollectionObserver<COLLECTION, ITEM extends IOInterface> {
    private final WeakHashMap<onCollectionUpdated<COLLECTION, ITEM>, Boolean> observers = new WeakHashMap<>();
    private AtomicBoolean blocked = new AtomicBoolean();

    final public void registerObserver(onCollectionUpdated o) {
        if (blocked.get())
            throw new RuntimeException();
        observers.put(o, true);
    }

    final public void unregisterObserver(onCollectionUpdated o) {
        if (blocked.get())
            throw new RuntimeException();
        observers.remove(o);
    }

    final protected void notifyObservers(ITEM item, ObserverUpdateActions actions) {
        blocked.set(true);
        Iterator<onCollectionUpdated<COLLECTION, ITEM>> it = observers.keySet().iterator();
        while (it.hasNext())
            if (!it.next().updated((COLLECTION) this, item, actions))
                it.remove();
        blocked.set(false);
    }

}
