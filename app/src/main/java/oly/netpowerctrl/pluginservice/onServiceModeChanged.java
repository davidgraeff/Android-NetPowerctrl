package oly.netpowerctrl.pluginservice;

/**
 * Refresh has started or is finished
 */
public interface onServiceModeChanged {
    void onServiceModeChanged(boolean isNetworkDown);
}
