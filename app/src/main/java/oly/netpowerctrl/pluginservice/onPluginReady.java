package oly.netpowerctrl.pluginservice;

/**
 * Get a notification when the service is ready or has finished
 */
public interface onPluginReady {
    void onPluginReady(PluginRemote plugin);

    void onPluginFailedToInit(PluginRemote plugin);

    void onPluginFinished(PluginRemote plugin);
}
