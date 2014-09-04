package oly.netpowerctrl.listen_service;

/**
 * Refresh has started or is finished
 */
public interface onServiceModeChanged {
    void onServiceModeChanged(boolean isNetworkDown);
}
