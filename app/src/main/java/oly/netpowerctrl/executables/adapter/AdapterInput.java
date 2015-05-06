package oly.netpowerctrl.executables.adapter;

import oly.netpowerctrl.data.DataService;

;

/**
 * Created by david on 07.07.14.
 */
public abstract class AdapterInput {
    protected AdapterSource adapterSource;

    abstract void doUpdateNow();

    abstract void onStart(DataService dataService);

    abstract void onFinish();

    void setAdapterSource(AdapterSource adapterSource) {
        this.adapterSource = adapterSource;
    }

}
