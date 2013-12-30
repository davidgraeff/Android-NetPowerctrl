package oly.netpowerctrl.datastructure;

import java.util.ArrayList;

import oly.netpowerctrl.main.NetpowerctrlApplication;

public class OutletCommand {
    // This field is not saved and is filled by fromOutletInfo for cache purposes only
    public String description;

    public String device_mac;
    public int outletNumber;
    public int state; //0:off;1:on;2:toggle
    public boolean enabled = false;
    public OutletInfo outletinfo = null;

    public String toString() {
        if (device_mac.isEmpty())
            return "";
        return description.replace("§", "$") + "§" + device_mac +
                "§" + Integer.valueOf(outletNumber).toString() + "§" + Integer.valueOf(state).toString();
    }

    public static OutletCommand fromString(String source) {
        OutletCommand c = new OutletCommand();
        String src[] = source.split("§");
        if (src.length < 3)
            return null;
        c.device_mac = src[1];
        c.outletNumber = Integer.valueOf(src[2]);
        c.state = Integer.valueOf(src[3]);
        c.description = c.device_mac + ":" + Integer.valueOf(c.outletNumber).toString();

        ArrayList<DeviceInfo> devices = NetpowerctrlApplication.instance.configuredDevices;
        for (DeviceInfo device : devices) {
            if (device.MacAddress.equals(c.device_mac)) {
                for (OutletInfo outlet : device.Outlets) {
                    if (outlet.OutletNumber == c.outletNumber) {
                        c.outletinfo = outlet;
                        c.description = outlet.device.DeviceName + ": " + (outlet.UserDescription.isEmpty() ? outlet.Description : outlet.UserDescription);
                        break;
                    }
                }
            }
        }
        return c;
    }

    public static OutletCommand fromOutletInfo(OutletInfo info, boolean enabled) {
        OutletCommand c = new OutletCommand();
        c.outletinfo = info;
        c.enabled = enabled;
        c.description = info.device.DeviceName + ": " + (info.UserDescription.isEmpty() ? info.Description : info.UserDescription);
        c.device_mac = info.device.MacAddress;
        c.outletNumber = info.OutletNumber;
        c.state = info.State ? 1 : 0;
        return c;
    }
}
