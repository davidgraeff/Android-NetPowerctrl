package oly.netpowerctrl.application_state;

/**
 * Refresh has started or is finished
 */
public interface RefreshStartedStopped {
    void onRefreshStateChanged(boolean isRefreshing);
}
