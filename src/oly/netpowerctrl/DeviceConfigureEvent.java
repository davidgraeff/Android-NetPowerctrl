package oly.netpowerctrl;

public interface DeviceConfigureEvent
{
    // Request this device to be configured
    public void onConfigureDevice (int position);
}