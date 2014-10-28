package oly.netpowerctrl.listen_service;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceReadyObserver extends Observer<onServiceReady> implements onServiceReady {
    @Override
    public void register(onServiceReady o) {
        super.register(o);
        if (ListenService.isServiceReady()) {
            o.onServiceReady(ListenService.getService());
        }
    }

    @Override
    public boolean onServiceReady(ListenService service) {
        Iterator<onServiceReady> iterator = listeners.keySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onServiceReady(service))
                iterator.remove();
        }
        return false;
    }

    @Override
    public void onServiceFinished() {
        for (onServiceReady listener : listeners.keySet()) {
            listener.onServiceFinished();
        }
    }
}
