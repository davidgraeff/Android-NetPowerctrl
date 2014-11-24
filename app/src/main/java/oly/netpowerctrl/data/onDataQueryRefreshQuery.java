package oly.netpowerctrl.data;

/**
 * Refresh has started or is finished
 */
public interface onDataQueryRefreshQuery {
    void onRefreshStateChanged(boolean isRefreshing);
}
