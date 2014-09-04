package oly.netpowerctrl.utils.controls;

import oly.netpowerctrl.data.IconDeferredLoadingThread;

/**
 * For activities with icon cache
 */
public interface ActivityWithIconCache {
    public IconDeferredLoadingThread getIconCache();
}
