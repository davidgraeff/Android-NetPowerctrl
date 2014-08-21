package oly.netpowerctrl.utils;

import java.util.ArrayList;

/**
 * Created by david on 19.08.14.
 */
public class Observer<T> {
    protected final ArrayList<T> listeners = new ArrayList<>();

    public void register(T o) {
        if (!listeners.contains(o)) {
            listeners.add(o);
        }
    }

    public void unregister(T o) {
        listeners.remove(o);
    }
}
