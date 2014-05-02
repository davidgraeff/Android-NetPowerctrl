package oly.netpowerctrl.application_state;

/**
 * Get a notification when the service is ready or has finished
 */
public interface ServiceReady {
    boolean onServiceReady();

    void onServiceFinished();
}
