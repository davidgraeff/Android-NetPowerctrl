package oly.netpowerctrl.application_state;

/**
 * Implement this interface to get notified if the data within the RuntimeDataController changes.
 */
public interface RuntimeStateChanged {
    boolean onDataLoaded();

    boolean onDataQueryFinished();
}
