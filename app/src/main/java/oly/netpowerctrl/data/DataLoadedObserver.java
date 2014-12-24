package oly.netpowerctrl.data;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class DataLoadedObserver extends Observer<onDataLoaded> implements onDataLoaded {
    boolean isLoaded = false;

    public void reset() {
        isLoaded = false;
    }

    public boolean isDone() {
        return isLoaded;
    }

    @Override
    public void register(onDataLoaded o) {
        boolean register = true;
        if (isLoaded) {
            // If the object return false we do not register it for further changes.
            register = o.onDataLoaded();
        }

        if (register)
            super.register(o);
    }

    @Override
    public boolean onDataLoaded() {
        isLoaded = true;
        Iterator<onDataLoaded> iterator = listeners.keySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onDataLoaded())
                iterator.remove();
        }
        return true;
    }
}
