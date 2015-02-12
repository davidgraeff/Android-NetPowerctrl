package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

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

        switch (action) {
            case RemoveAction:
                //Log.w("REMOVE source ports", device.getDeviceName());
                device.lockDevicePorts();
                Iterator<DevicePort> it = device.getDevicePortIterator();
                while (it.hasNext()) {
                    adapterSource.removeAt(adapterSource.findPositionByUUid(it.next().getUid()));
                }
                device.releaseDevicePorts();
                break;
            case UpdateAction:
                //Log.w("UPDATE source ports", device.getDeviceName());
                device.lockDevicePorts();
                Iterator<DevicePort> iterator = device.getDevicePortIterator();
                while (iterator.hasNext()) {
                    int pos = adapterSource.findPositionByUUid(iterator.next().getUid());
                    if (pos != -1) adapterSource.getItem(pos).markRemoved();
                }
                iterator = device.getDevicePortIterator();
                while (iterator.hasNext()) {
                    DevicePort devicePort = iterator.next();
                    if (devicePort.isHidden())
                        continue;
                    adapterSource.addItem(devicePort, devicePort.current_value);
                }
                device.releaseDevicePorts();
                adapterSource.removeAllMarked();
                break;
            case AddAction:
                device.lockDevicePorts();
                iterator = device.getDevicePortIterator();
                while (iterator.hasNext()) {
                    DevicePort devicePort = iterator.next();
                    if (devicePort.isHidden())
                        continue;
                    adapterSource.addItem(devicePort, devicePort.current_value);
                }
                device.releaseDevicePorts();
                break;
            case ClearAndNewAction:
            case RemoveAllAction:
                //Log.w("CLEAR source ports", device.getDeviceName());
                adapterSource.updateNow();
                break;
        }

        adapterSource.sourceChanged();

        return true;
    }
}
