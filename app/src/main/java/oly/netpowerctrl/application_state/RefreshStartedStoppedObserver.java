package oly.netpowerctrl.application_state;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class RefreshStartedStoppedObserver extends Observer<onRefreshStartedStopped> implements onRefreshStartedStopped {
    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        for (onRefreshStartedStopped listener : listeners) {
            listener.onRefreshStateChanged(isRefreshing);
        }
    }
}
