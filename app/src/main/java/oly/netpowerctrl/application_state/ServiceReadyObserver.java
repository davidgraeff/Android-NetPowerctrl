package oly.netpowerctrl.application_state;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceReadyObserver extends Observer<ServiceReady> implements ServiceReady {
    @Override
    public void register(ServiceReady o) {
        super.register(o);
        if (NetpowerctrlService.isServiceReady()) {
            onServiceReady(NetpowerctrlService.getService());
        }
    }

    @Override
    public boolean onServiceReady(NetpowerctrlService service) {
        Iterator<ServiceReady> it = listeners.iterator();
        while (it.hasNext()) {
            if (!it.next().onServiceReady(service))
                it.remove();
        }
        return false;
    }

    @Override
    public void onServiceFinished() {
        for (ServiceReady listener : listeners) {
            listener.onServiceFinished();
        }
    }
}
