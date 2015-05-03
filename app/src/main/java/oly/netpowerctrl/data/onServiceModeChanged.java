package oly.netpowerctrl.data;

/**
 * Refresh has started or is finished
 */
public interface onServiceModeChanged {
    void onServiceModeChanged(boolean isNetworkDown);
}
