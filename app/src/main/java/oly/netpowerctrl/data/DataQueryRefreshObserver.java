package oly.netpowerctrl.data;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class DataQueryRefreshObserver extends Observer<onDataQueryRefreshQuery> implements onDataQueryRefreshQuery {
    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        for (onDataQueryRefreshQuery listener : listeners.keySet()) {
            listener.onRefreshStateChanged(isRefreshing);
        }
    }
}
