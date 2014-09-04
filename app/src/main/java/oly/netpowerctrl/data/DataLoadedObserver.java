package oly.netpowerctrl.data;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class DataLoadedObserver extends Observer<onDataLoaded> implements onDataLoaded {
    public boolean dataLoaded = false;

    @Override
    public void register(onDataLoaded o) {
        boolean register = true;
        if (dataLoaded) {
            // If the object return false we do not register it for further changes.
            register = o.onDataLoaded();
        }

        if (register)
            super.register(o);
    }

    @Override
    public boolean onDataLoaded() {
        dataLoaded = true;
        Iterator<onDataLoaded> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onDataLoaded())
                iterator.remove();
        }
        return true;
    }
}
