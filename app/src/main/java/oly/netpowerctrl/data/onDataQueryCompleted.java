package oly.netpowerctrl.data;

/**
 * Implement this interface to get notified if the data within the RuntimeDataController changes.
 */
public interface onDataQueryCompleted {
    boolean onDataQueryFinished(AppData appData, boolean networkDevicesNotReachable);
}
