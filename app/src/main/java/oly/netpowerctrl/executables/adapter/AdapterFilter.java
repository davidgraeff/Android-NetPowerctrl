package oly.netpowerctrl.executables.adapter;

import oly.netpowerctrl.executables.Executable;

/**
 * Created by david on 30.12.14.
 */
public abstract class AdapterFilter {
    protected AdapterSource adapterSource;

    public void setAdapterSource(AdapterSource adapterSource) {
        this.adapterSource = adapterSource;
    }

    abstract boolean filter(Executable executable);
}
