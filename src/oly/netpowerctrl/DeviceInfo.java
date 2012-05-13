package oly.netpowerctrl;

import java.util.List;

// this class holds all the info about one device
public class DeviceInfo {

	public String DeviceName; // name of the device as reported by UDP or configured by the user
	public String HostName;   // the hostname or ip address used to reach the device
	
	public List<OutletInfo> Outlets;
	
	
}
