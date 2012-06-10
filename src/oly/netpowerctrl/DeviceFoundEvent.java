package oly.netpowerctrl;

public interface DeviceFoundEvent {
    // a new device was found on the network.
    public void onDeviceFound (DeviceInfo device_info);
}
