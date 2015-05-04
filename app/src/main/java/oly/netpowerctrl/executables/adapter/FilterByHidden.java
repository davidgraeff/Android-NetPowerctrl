package oly.netpowerctrl.executables.adapter;

import oly.netpowerctrl.executables.Executable;

/**
 * Created by david on 30.12.14.
 */
public class FilterByHidden extends AdapterSourceFilter {
    protected boolean hideHidden = false;

    public FilterByHidden(boolean hideHidden) {
        this.hideHidden = hideHidden;
    }

    public boolean isHideHidden() {
        return hideHidden;
    }

    public void setHideHidden(boolean hideHidden) {
        this.hideHidden = hideHidden;
    }

    @Override
    public boolean filter(Executable executable) {
        return hideHidden && executable.isHidden();
    }
}
