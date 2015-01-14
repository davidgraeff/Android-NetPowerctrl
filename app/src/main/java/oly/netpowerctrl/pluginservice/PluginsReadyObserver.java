package oly.netpowerctrl.pluginservice;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.utils.Logging;

/**
 * Created by david on 19.08.14.
 */
class PluginsReadyObserver {
    private final PluginService pluginService;
    private final List<AbstractBasePlugin> plugins_not_activated = new ArrayList<>();
    private int pluginCount = 0;

    public PluginsReadyObserver(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    public void addPluginCount(int pluginCount) {
        this.pluginCount += pluginCount;
    }

    public void decreasePluginCount() {
        --this.pluginCount;
        if (pluginCount <= 0) {
            pluginService.onPluginsReady(pluginService, plugins_not_activated);
        }
    }

    public boolean isLoaded() {
        return pluginCount == 0;
    }

    public void add(AbstractBasePlugin plugin) {
        plugins_not_activated.add(plugin);
        plugin.registerReadyObserver(new onPluginReady() {
            @Override
            public void onPluginReady(AbstractBasePlugin plugin, boolean withErrors) {
                if (withErrors) {
                    Logging.getInstance().logExtensions("Fehler: " + plugin.getLocalizedName());
                } else {
                    Logging.getInstance().logExtensions("Aktiv: " + plugin.getLocalizedName());
                }
                decreasePluginCount();
            }
        });
    }

    public void remove(AbstractBasePlugin plugin) {
        plugins_not_activated.remove(plugin);
    }

    public void reset() {
        plugins_not_activated.clear();
        pluginCount = 0;
    }

    public void checkIfReady() {
        if (pluginCount <= 0 && plugins_not_activated.size() > 0) {
            pluginService.onPluginsReady(pluginService, plugins_not_activated);
        }
    }
}
