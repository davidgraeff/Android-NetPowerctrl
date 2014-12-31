package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * A source for the ExecutablesBaseAdapter. If you add this source to the adapter all devicePorts,
 * even hidden, from the given device are added to the adapter.
 */
public class AdapterSourceInputOneDevicePorts extends AdapterSourceInput {
    private Device device;
    private Set<String> shownPorts = new TreeSet<>();

    public AdapterSourceInputOneDevicePorts(@NonNull Device device) {
        this.device = device;
    }

    @Override
    void setAdapterSource(AdapterSource adapterSource) {
        super.setAdapterSource(adapterSource);
        device.lockDevicePorts();
        Iterator<DevicePort> iterator = device.getDevicePortIterator();
        while (iterator.hasNext()) {
            DevicePort devicePort = iterator.next();
            if (!devicePort.isHidden())
                shownPorts.add(devicePort.getUid());
            adapterSource.addItem(devicePort, devicePort.current_value);
        }
        device.releaseDevicePorts();
    }

    @Override
    public void doUpdateNow() {
    }

    @Override
    void onStart(AppData appData) {
    }

    @Override
    void onFinish() {
    }

    /**
     * @return Return an association between multiple DevicePorts and their respective checked flag.
     */
    public Set<String> shownDevicePorts() {
        return shownPorts;
    }
}
