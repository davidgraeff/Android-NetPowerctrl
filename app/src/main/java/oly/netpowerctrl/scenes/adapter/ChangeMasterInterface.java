package oly.netpowerctrl.scenes.adapter;

import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;

/**
 * Created by david on 10.05.15.
 */
interface ChangeMasterInterface {
    ExecutableAdapterItem getMaster();

    void setMaster(ExecutableAdapterItem item);

}
