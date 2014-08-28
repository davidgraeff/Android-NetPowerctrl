package oly.netpowerctrl.application_state;

/**
 * Refresh has started or is finished
 */
public interface onServiceModeChanged {
    void onServiceModeChanged(boolean isNetworkDown);
}
