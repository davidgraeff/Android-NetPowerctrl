package oly.netpowerctrl.data.query;

import oly.netpowerctrl.data.DataService;

/**
 * Implement this interface to get notified if the data within the RuntimeDataController changes.
 */
public interface onDataQueryCompleted {
    boolean onDataQueryFinished(DataService dataService);
}
