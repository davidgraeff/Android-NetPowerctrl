package oly.netpowerctrl.application_state;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceModeChangedObserver extends Observer<ServiceModeChanged> implements ServiceModeChanged {
    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        for (ServiceModeChanged listener : listeners) {
            listener.onServiceModeChanged(isNetworkDown);
        }
    }
}
