package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.UUID;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * A source for the ExecutablesBaseAdapter. If you add this source to the adapter all devicePorts,
 * even hidden, from the given device are added to the adapter.
 */
public class ExecutablesSourceOneDevicePorts extends ExecutablesSourceBase {
    private Device device;
    private Boolean[] shownPorts;

    public ExecutablesSourceOneDevicePorts(@Nullable ExecutablesSourceChain executablesSourceChain,
                                           @NonNull Device device) {
        super(executablesSourceChain);
        this.device = device;
    }

    @Override
    public int doCountIfGroup(UUID uuid) {
        return 0;
    }

    @Override
    public void fullUpdate(ExecutablesBaseAdapter adapter) {
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

    /**
     * @return Return an array of booleans for each DevicePort one entry,
     * where true stand for a visible DevicePort and false for a hidden one.
     */
    public Boolean[] shownDevicePorts() {
        return shownPorts;
    }

    @Override
    protected void automaticUpdatesDisable() {
    }

    @Override
    protected void automaticUpdatesEnable() {
    }
}
