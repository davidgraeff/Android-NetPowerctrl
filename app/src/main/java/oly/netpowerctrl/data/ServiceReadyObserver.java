package oly.netpowerctrl.data;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceReadyObserver extends Observer<onServiceReady> implements onServiceReady {
    public boolean isReady = false;
    @Override
    public void register(onServiceReady o) {
        super.register(o);
        if (isReady) {
            o.onServiceReady(DataService.getService());
        }
    }

    @Override
    public boolean onServiceReady(DataService service) {
        isReady = true;
        Iterator<onServiceReady> iterator = listeners.keySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onServiceReady(service))
                iterator.remove();
        }
        return false;
    }

    @Override
    public void onServiceFinished(DataService service) {
        isReady = false;
        for (onServiceReady listener : listeners.keySet()) {
            listener.onServiceFinished(service);
        }
    }
}
