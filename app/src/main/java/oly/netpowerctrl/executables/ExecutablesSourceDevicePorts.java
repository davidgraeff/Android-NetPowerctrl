package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * Created by david on 07.07.14.
 */
public class ExecutablesSourceDevicePorts extends ExecutablesSourceBase implements onCollectionUpdated<Object, Device>, onDataQueryCompleted, onDataLoaded {
    private List<DevicePort> mList = new ArrayList<>();

    public ExecutablesSourceDevicePorts(ExecutablesSourceChain executablesSourceChain) {
        super(executablesSourceChain);
    }

    @Override
    public int doCountIfGroup(UUID uuid) {
        int c = 0;
        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;

            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (!devicePort.isHidden() && (uuid == null && devicePort.groups.size() == 0 || devicePort.groups.contains(uuid)))
                    ++c;
            }
            device.releaseDevicePorts();
        }
        return c;
    }

    @Override
    public void fullUpdate(ExecutablesBaseAdapter adapter) {

        mList.clear();
        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;

            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.isHidden())
                    continue;
                mList.add(devicePort);
            }
            device.releaseDevicePorts();
        }

        if (adapter == null) {
            automaticUpdatesDisable();
        } else {
            for (DevicePort devicePort : mList)
                if (!hideNotReachable || devicePort.isReachable())
                    adapter.addItem(devicePort, devicePort.current_value);
        }
    }

    @Override
    protected void automaticUpdatesDisable() {
        AppData.observersDataQueryCompleted.unregister(this);
        AppData.getInstance().deviceCollection.unregisterObserver(this);
    }

    @Override
    protected void automaticUpdatesEnable() {
        AppData.observersDataQueryCompleted.register(this);
        // If no data has been loaded so far, wait for load action to be completed before
        // registering to deviceCollection changes.
        if (!AppData.isDataLoaded())
            AppData.observersOnDataLoaded.register(this);
        else {
            AppData.getInstance().deviceCollection.registerObserver(this);
        }
    }

    @Override
    public boolean onDataQueryFinished(boolean networkDevicesNotReachable) {
        //updateNow();
        return true;
    }

    @Override
    public boolean onDataLoaded() {
        if (automaticUpdatesEnabled)
            automaticUpdatesEnable();
        return true;
    }

    @Override
    public boolean updated(@NonNull Object collection, Device device, @NonNull ObserverUpdateActions action, int position) {
        if (adapterWeakReference == null || device == null)
            return true;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction || (hideNotReachable && device.getFirstReachableConnection() == null)) {
            Log.w("REMOVE source ports", device.getDeviceName());
            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                adapter.removeAt(findPositionByUUid(adapter, it.next().getUid()));
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.AddAction || action == ObserverUpdateActions.UpdateAction) {
            Log.w("UPDATE source ports", device.getDeviceName());
            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.isHidden())
                    continue;
                adapter.addItem(devicePort, devicePort.current_value);
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.ClearAndNewAction || action == ObserverUpdateActions.RemoveAllAction) {
            Log.w("CLEAR source ports", device.getDeviceName());
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
