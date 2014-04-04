package oly.netpowerctrl.network;

/**
 * Get a notification when the service is ready or has finished
 */
public interface ServiceReady {
    boolean onServiceReady(NetpowerctrlService mDiscoverService);

    void onServiceFinished();
}
