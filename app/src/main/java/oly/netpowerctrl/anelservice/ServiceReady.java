package oly.netpowerctrl.anelservice;

/**
 * Get a notification when the service is ready
 */
public interface ServiceReady {
    void onServiceReady(NetpowerctrlService mDiscoverService);
}
