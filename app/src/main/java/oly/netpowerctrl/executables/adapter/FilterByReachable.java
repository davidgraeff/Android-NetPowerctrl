package oly.netpowerctrl.executables.adapter;

import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.scenes.Scene;

/**
 * Created by david on 30.12.14.
 */
public class FilterByReachable extends AdapterFilter {
    protected boolean hideNotReachable = false;
    private boolean showNotReachableScenes = true;

    public FilterByReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    public void setShowNotReachableScenes(boolean showNotReachableScenes) {
        this.showNotReachableScenes = showNotReachableScenes;
    }

    public boolean isHideNotReachable() {
        return hideNotReachable;
    }

    public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    @Override
    public boolean filter(Executable executable) {
        if (executable instanceof Scene)
            return !showNotReachableScenes &&
                    executable.reachableState() == ReachabilityStates.NotReachable;
        return hideNotReachable && executable.reachableState() == ReachabilityStates.NotReachable;
    }
}
