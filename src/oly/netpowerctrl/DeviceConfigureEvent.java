package oly.netpowerctrl;


public interface DeviceConfigureEvent
{
	public enum ConfType {ConfiguredDevice, DiscoveredDevice};
	
    // Request this device to be configured
    public void onConfigureDevice (ConfType type, int position);
}