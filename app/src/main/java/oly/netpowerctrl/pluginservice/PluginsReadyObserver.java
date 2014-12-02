package oly.netpowerctrl.pluginservice;

import java.util.Iterator;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class PluginsReadyObserver extends Observer<onPluginsReady> {
    private int pluginCount = 0;

    public void setPluginCount(int pluginCount) {
        this.pluginCount = pluginCount;
    }

    public void decreasePluginCount() {
        --this.pluginCount;
        if (pluginCount <= 0) {
            Iterator<onPluginsReady> iterator = listeners.keySet().iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().onPluginsReady())
                    iterator.remove();
            }
        }
    }

    @Override
    public void register(onPluginsReady o) {
        super.register(o);
        if (pluginCount <= 0) {
            o.onPluginsReady();
        }
    }

    public boolean isLoaded() {
        return pluginCount == 0;
    }
}
