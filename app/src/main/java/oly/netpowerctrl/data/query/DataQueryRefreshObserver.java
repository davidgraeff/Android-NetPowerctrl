package oly.netpowerctrl.data.query;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class DataQueryRefreshObserver extends Observer<onDataQueryRefreshQuery> implements onDataQueryRefreshQuery {
    private boolean mIsRefreshing = false;
    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        mIsRefreshing = isRefreshing;
        for (onDataQueryRefreshQuery listener : listeners.keySet()) {
            listener.onRefreshStateChanged(isRefreshing);
        }
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }
}
