package oly.netpowerctrl.data;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class DataQueryCompletedObserver extends Observer<onDataQueryCompleted> implements onDataQueryCompleted {
    private boolean initialDataQueryCompleted = false;

    public void resetDataQueryCompleted() {
        initialDataQueryCompleted = false;
    }

    /**
     * @param o The callback object
     *          If the initial data query already finished, you will be
     *          notified immediately. Depending on the result of the
     *          callback method your object will either be registered
     *          or not.
     */
    @Override
    public void register(onDataQueryCompleted o) {
        boolean register = true;
        if (initialDataQueryCompleted) {
            // If the object return false we do not register it for further changes.
            register = o.onDataQueryFinished();
        }

        if (register)
            super.register(o);
    }

    @Override
    public boolean onDataQueryFinished() {
        initialDataQueryCompleted = true;
        Iterator<onDataQueryCompleted> iterator = listeners.keySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onDataQueryFinished())
                iterator.remove();
        }
        return true;
    }
}
