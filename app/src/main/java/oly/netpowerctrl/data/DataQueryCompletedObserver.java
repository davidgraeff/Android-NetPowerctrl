package oly.netpowerctrl.data;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Register to this observer via AppData and get informed if the initial data query for all stored
 * devices is done. Usefull if you want to e.g. refresh a device list where it makes sense to wait
 * for all device states to settle first.
 */
public class DataQueryCompletedObserver extends Observer<onDataQueryCompleted> implements onDataQueryCompleted {
    private WeakReference<AppData> appDataWeakReference = new WeakReference<>(null);

    public void reset() {
        appDataWeakReference = new WeakReference<>(null);
    }

    public boolean isDone() {
        return appDataWeakReference.get() != null;
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
        AppData appData = appDataWeakReference.get();
        if (appData != null) {
            // If the object return false we do not register it for further changes.
            register = o.onDataQueryFinished(appData);
        }

        if (register)
            super.register(o);
    }

    @Override
    public boolean onDataQueryFinished(AppData appData) {
        appDataWeakReference = new WeakReference<>(appData);
        Iterator<onDataQueryCompleted> iterator = listeners.keySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onDataQueryFinished(appData))
                iterator.remove();
        }
        return true;
    }
}
