package oly.netpowerctrl.application_state;

/**
 * Get a notification when the service is ready or has finished
 */
public interface onServiceReady {
    boolean onServiceReady(NetpowerctrlService service);

    void onServiceFinished();
}
