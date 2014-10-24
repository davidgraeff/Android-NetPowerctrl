package oly.netpowerctrl.executables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DevicePort;

/**
 * Created by david on 07.07.14.
 */
public class ExecutablesSourceDevicePorts extends ExecutablesSourceBase implements onCollectionUpdated<Object, Device>, onDataLoaded {
    private List<DevicePort> mList = new ArrayList<>();

    @Override
    public void fullUpdate(ExecutablesBaseAdapter adapter) {
        super.fullUpdate(adapter);

        mList.clear();
        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;

            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.Disabled)
                    continue;
                mList.add(devicePort);
            }
            device.releaseDevicePorts();
        }

        if (adapter == null) {
            automaticUpdatesDisable();
        } else {
            for (DevicePort devicePort : mList)
                adapter.addItem(devicePort, devicePort.current_value);
        }
    }

    @Override
    protected void automaticUpdatesDisable() {
        AppData.getInstance().deviceCollection.unregisterObserver(this);
    }

    @Override
    protected void automaticUpdatesEnable() {
        // If no data has been loaded so far, wait for load action to be completed before
        // registering to deviceCollection changes.
        if (!AppData.observersOnDataLoaded.dataLoaded)
            AppData.observersOnDataLoaded.register(this);
        else {
            AppData.getInstance().deviceCollection.registerObserver(this);
        }
    }

    @Override
    public boolean onDataLoaded() {
        if (automaticUpdatesEnabled)
            automaticUpdatesEnable();
        return false;
    }

    @Override
    public boolean updated(Object collection, Device device, ObserverUpdateActions action, int position) {
        if (adapterWeakReference == null || device == null)
            return true;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction || (hideNotReachable && device.getFirstReachableConnection() == null)) {

            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                adapter.removeAt(findPositionByUUid(adapter, it.next().getUid()));
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.AddAction || action == ObserverUpdateActions.UpdateAction) {

            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.Disabled)
                    continue;
                adapter.addItem(devicePort, devicePort.current_value);
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.ClearAndNewAction || action == ObserverUpdateActions.RemoveAllAction) {
            updateNow();
            return true;
        }

        if (onChangeListener != null)
            onChangeListener.sourceChanged();

        return true;
    }

    private int findPositionByUUid(ExecutablesBaseAdapter adapter, String uuid) {
        if (uuid == null)
            return -1;

        int i = -1;
        for (ExecutableAdapterItem info : adapter.mItems) {
            ++i;
            String uid = info.getExecutableUid();
            if (uid == null) // skip header items
                continue;
            if (uid.equals(uuid))
                return i;
        }

        return -1;
    }

    public List<DevicePort> getDevicePortList() {
        return mList;
    }

    public int indexOf(DevicePort port) {
        for (int i = 0; i < mList.size(); ++i)
            if (mList.get(i) == port)
                return i;
        return -1;
    }

}
