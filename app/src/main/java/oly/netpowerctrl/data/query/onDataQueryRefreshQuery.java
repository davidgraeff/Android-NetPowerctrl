package oly.netpowerctrl.data.query;

/**
 * Refresh has started or is finished
 */
public interface onDataQueryRefreshQuery {
    void onRefreshStateChanged(boolean isRefreshing);
}
