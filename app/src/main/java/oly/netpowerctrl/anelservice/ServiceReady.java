package oly.netpowerctrl.anelservice;

/**
 * Get a notification when the service is ready
 */
public interface ServiceReady {
    boolean onServiceReady(NetpowerctrlService mDiscoverService);
}
