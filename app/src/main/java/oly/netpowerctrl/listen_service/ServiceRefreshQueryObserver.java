package oly.netpowerctrl.listen_service;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class ServiceRefreshQueryObserver extends Observer<onServiceRefreshQuery> implements onServiceRefreshQuery {
    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        for (onServiceRefreshQuery listener : listeners) {
            listener.onRefreshStateChanged(isRefreshing);
        }
    }
}
