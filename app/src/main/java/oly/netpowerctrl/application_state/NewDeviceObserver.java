package oly.netpowerctrl.application_state;

import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.network.onNewDevice;
import oly.netpowerctrl.utils.Observer;

/**
 * Created by david on 19.08.14.
 */
public class NewDeviceObserver extends Observer<onNewDevice> implements onNewDevice {
    @Override
    public void onNewDevice(Device device) {
        for (onNewDevice listener : listeners) {
            listener.onNewDevice(device);
        }
    }
}
