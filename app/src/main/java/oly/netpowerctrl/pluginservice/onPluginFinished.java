package oly.netpowerctrl.pluginservice;

/**
 * Get a notification when the service is ready or has finished
 */
public interface onPluginFinished {
    void onPluginFinished(AbstractBasePlugin plugin);
}
