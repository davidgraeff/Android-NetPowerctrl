package oly.netpowerctrl.executables;

import oly.netpowerctrl.device_base.executables.Executable;

/**
 * Created by david on 30.12.14.
 */
public abstract class AdapterSourceFilter {
    protected AdapterSource adapterSource;

    public void setAdapterSource(AdapterSource adapterSource) {
        this.adapterSource = adapterSource;
    }

    abstract boolean filter(Executable executable);
}
