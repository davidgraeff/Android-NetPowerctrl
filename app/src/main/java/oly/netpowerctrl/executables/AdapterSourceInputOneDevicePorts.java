package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

import java.util.Iterator;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * A source for the ExecutablesBaseAdapter. If you add this source to the adapter all devicePorts,
 * even hidden, from the given device are added to the adapter.
 */
public class AdapterSourceInputOneDevicePorts extends AdapterSourceInput {
    private Device device;
    private Boolean[] shownPorts;

    public AdapterSourceInputOneDevicePorts(@NonNull Device device) {
        this.device = device;
    }

    @Override
    public void doUpdateNow(@NonNull ExecutablesBaseAdapter adapter) {
        device.lockDevicePorts();
        shownPorts = new Boolean[device.countDevicePorts()];
        int c = 0;
        Iterator<DevicePort> iterator = device.getDevicePortIterator();
        while (iterator.hasNext()) {
            DevicePort devicePort = iterator.next();
            shownPorts[c++] = !devicePort.isHidden();
            adapter.addItem(devicePort, devicePort.current_value);
        }
        device.releaseDevicePorts();
    }

    @Override
    void onStart(AppData appData) {

    }

    @Override
    void onFinish() {

    }

    /**
     * @return Return an array of booleans for each DevicePort one entry,
     * where true stand for a visible DevicePort and false for a hidden one.
     */
    public Boolean[] shownDevicePorts() {
        return shownPorts;
    }
}
