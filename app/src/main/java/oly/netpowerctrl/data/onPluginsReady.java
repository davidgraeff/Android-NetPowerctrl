package oly.netpowerctrl.data;

import java.util.List;

/**
 * Get a notification when all plugins are correctly registered (or failed)
 */
public interface onPluginsReady {
    boolean onPluginsReady(DataService dataService, List<AbstractBasePlugin> not_activated_plugins);
}
