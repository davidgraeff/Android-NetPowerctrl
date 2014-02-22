package oly.netpowerctrl.shortcut;

import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsBaseAdapter;

/**
 * Created by david on 17.02.14.
 */
public interface OutletsManipulator {
    void setManipulatorObjects(int tag, DynamicGridView view, DevicePortsBaseAdapter adapter);
}
