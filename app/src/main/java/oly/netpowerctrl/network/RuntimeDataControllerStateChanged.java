package oly.netpowerctrl.network;

/**
 * Implement this interface to get notified if the data within the RuntimeDataController changes.
 */
public interface RuntimeDataControllerStateChanged {
    void onDataReloaded();

    void onDataQueryFinished();
}
