package oly.netpowerctrl.application_state;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceReadyObserver extends Observer<onServiceReady> implements onServiceReady {
    @Override
    public void register(onServiceReady o) {
        super.register(o);
        if (NetpowerctrlService.isServiceReady()) {
            o.onServiceReady(NetpowerctrlService.getService());
        }
    }

    @Override
    public boolean onServiceReady(NetpowerctrlService service) {
        Iterator<onServiceReady> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().onServiceReady(service))
                iterator.remove();
        }
        return false;
    }

    @Override
    public void onServiceFinished() {
        for (onServiceReady listener : listeners) {
            listener.onServiceFinished();
        }
    }
}
