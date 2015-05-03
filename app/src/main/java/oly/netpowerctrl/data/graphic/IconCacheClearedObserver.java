package oly.netpowerctrl.data.graphic;

import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class IconCacheClearedObserver extends Observer<IconCacheCleared> implements IconCacheCleared {
    @Override
    public void onIconCacheCleared() {
        for (IconCacheCleared listener : listeners.keySet()) {
            listener.onIconCacheCleared();
        }
    }
}
