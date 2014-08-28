package oly.netpowerctrl.application_state;

/**
 * Refresh has started or is finished
 */
public interface onRefreshStartedStopped {
    void onRefreshStateChanged(boolean isRefreshing);
}
