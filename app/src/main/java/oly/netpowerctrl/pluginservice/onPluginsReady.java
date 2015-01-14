package oly.netpowerctrl.pluginservice;

import java.util.List;

/**
 * Get a notification when all plugins are correctly registered (or failed)
 */
public interface onPluginsReady {
    boolean onPluginsReady(PluginService pluginService, List<AbstractBasePlugin> not_activated_plugins);
}
