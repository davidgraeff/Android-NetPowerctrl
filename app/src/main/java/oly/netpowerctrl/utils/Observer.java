package oly.netpowerctrl.utils;

import java.util.WeakHashMap;

/**
 * Created by david on 19.08.14.
 */
public class Observer<T> {
    protected final WeakHashMap<T, Boolean> listeners = new WeakHashMap<>();

    public void register(T o) {
        if (!listeners.containsKey(o)) {
            listeners.put(o, true);
        }
    }

    public void unregister(T o) {
        listeners.remove(o);
    }
}
