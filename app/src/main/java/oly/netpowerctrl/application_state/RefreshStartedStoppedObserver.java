package oly.netpowerctrl.application_state;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class RefreshStartedStoppedObserver extends Observer<RefreshStartedStopped> implements RefreshStartedStopped {
    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        for (RefreshStartedStopped listener : listeners) {
            listener.onRefreshStateChanged(isRefreshing);
        }
    }
}
