package oly.netpowerctrl.data;

/**
 * Get a notification when the service is ready or has finished
 */
public interface onPluginReady {
    void onPluginReady(AbstractBasePlugin plugin, boolean withErrors);
}
