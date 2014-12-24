package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputDevicePorts extends AdapterSourceInput implements onCollectionUpdated<Object, Device> {
    private List<DevicePort> mList = new ArrayList<>();
    private DeviceCollection deviceCollection = null;

    @Override
    public void doUpdateNow(@NonNull ExecutablesBaseAdapter adapter) {
        mList.clear();
        for (Device device : deviceCollection.getItems()) {
            if (adapterSource.hideNotReachable && device.getFirstReachableConnection() == null)
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

        for (DevicePort devicePort : mList)
            if (!adapterSource.hideNotReachable || devicePort.isReachable())
                adapter.addItem(devicePort, devicePort.current_value);
    }

    @Override
    void onStart(AppData appData) {
        this.deviceCollection = appData.deviceCollection;
        deviceCollection.registerObserver(this);
    }

    @Override
    void onFinish() {
        if (deviceCollection != null) deviceCollection.unregisterObserver(this);
        deviceCollection = null;
    }


    @Override
    public boolean updated(@NonNull Object collection, Device device, @NonNull ObserverUpdateActions action, int position) {
        if (device == null || adapterSource.ignoreUpdatesExecutable == device)
            return true;

        ExecutablesBaseAdapter adapter = adapterSource.getAdapter();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction || (adapterSource.hideNotReachable && device.getFirstReachableConnection() == null)) {
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
            adapterSource.updateNow();
            return true;
        }

        adapterSource.sourceChanged();

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
