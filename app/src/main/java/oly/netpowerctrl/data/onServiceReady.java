package oly.netpowerctrl.data;

/**
 * Get a notification when the service is ready (all plugins are ready, data is loaded) or has finished
 */
public interface onServiceReady {
    boolean onServiceReady(DataService service);

    void onServiceFinished(DataService service);
}
