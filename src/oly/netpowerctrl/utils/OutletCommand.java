package oly.netpowerctrl.utils;

public class OutletCommand {
	public String Description;
	public String device_mac;
	public int outletNumber;
	public int state; //0:off;1:on;2:toggle
	public boolean enabled = false;
	
	public String toString() {
		if (Description.isEmpty() || device_mac.isEmpty())
			return "";
		return Description.replace("§", "$") + "§" + device_mac +
				"§" + Integer.valueOf(outletNumber).toString() + "§" + Integer.valueOf(state).toString();
	}
	
	public static OutletCommand fromString(String source) {
		OutletCommand c = new OutletCommand();
		String src[] = source.split("§");
		if (src.length<3)
			return null;
		c.Description = src[0];
		c.device_mac = src[1];
		c.outletNumber = Integer.valueOf(src[2]);
		c.state = Integer.valueOf(src[3]);
		return c;
	}
	
	public static OutletCommand fromOutletInfo(OutletInfo info, boolean enabled) {
		OutletCommand c = new OutletCommand();
		c.enabled = enabled;
		c.Description = info.Description;
		c.device_mac = info.device.MacAddress;
		c.outletNumber = info.OutletNumber;
		c.state = info.State ? 1 : 0;
		return c;
	}
}
