package oly.netpowerctrl.application_state;

/**
 * Refresh has started or is finished
 */
public interface ServiceModeChanged {
    void onServiceModeChanged(boolean isNetworkDown);
}
