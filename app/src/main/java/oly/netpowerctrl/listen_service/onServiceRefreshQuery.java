package oly.netpowerctrl.listen_service;

/**
 * Refresh has started or is finished
 */
public interface onServiceRefreshQuery {
    void onRefreshStateChanged(boolean isRefreshing);
}
