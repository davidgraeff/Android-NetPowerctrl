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
    private DeviceCollection deviceCollection = null;

    @Override
    public void doUpdateNow() {
        List<DevicePort> devicePortList = new ArrayList<>();

        for (Device device : deviceCollection.getItems()) {
            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.isHidden())
                    continue;
                devicePortList.add(devicePort);
            }
            device.releaseDevicePorts();
        }

        for (DevicePort devicePort : devicePortList)
            adapterSource.addItem(devicePort, devicePort.current_value);
    }

    @Override
    void onStart(AppData appData) {
        this.deviceCollection = appData.deviceCollection;
        deviceCollection.registerObserver(this);
        doUpdateNow();
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

        if (action == ObserverUpdateActions.RemoveAction) {
            Log.w("REMOVE source ports", device.getDeviceName());
            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                adapterSource.removeAt(adapterSource.findPositionByUUid(it.next().getUid()));
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
                adapterSource.addItem(devicePort, devicePort.current_value);
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
}
