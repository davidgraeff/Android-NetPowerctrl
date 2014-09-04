package oly.netpowerctrl.listen_service;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceModeChangedObserver extends Observer<onServiceModeChanged> implements onServiceModeChanged {
    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        for (onServiceModeChanged listener : listeners) {
            listener.onServiceModeChanged(isNetworkDown);
        }
    }
}
