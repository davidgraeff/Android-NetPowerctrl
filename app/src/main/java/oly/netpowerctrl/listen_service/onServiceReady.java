package oly.netpowerctrl.listen_service;

/**
 * Get a notification when the service is ready or has finished
 */
public interface onServiceReady {
    boolean onServiceReady(ListenService service);

    void onServiceFinished();
}
