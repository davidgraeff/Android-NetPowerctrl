package oly.netpowerctrl.executables;

import oly.netpowerctrl.data.AppData;

/**
 * Created by david on 07.07.14.
 */
public abstract class AdapterSourceInput {
    protected AdapterSource adapterSource;

    abstract void doUpdateNow();

    abstract void onStart(AppData appData);

    abstract void onFinish();

    void setAdapterSource(AdapterSource adapterSource) {
        this.adapterSource = adapterSource;
    }
}
