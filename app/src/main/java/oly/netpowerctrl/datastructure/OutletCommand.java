package oly.netpowerctrl.datastructure;

public class OutletCommand {
    // This field is not saved and is filled by fromOutletInfo for cache purposes only
    public String description;

    public String device_mac;
    public int outletNumber;
    public int state; //0:off;1:on;2:toggle
    public boolean enabled = false;

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
        c.description = src[0];
        c.device_mac = src[1];
        c.outletNumber = Integer.valueOf(src[2]);
        c.state = Integer.valueOf(src[3]);
        return c;
    }

    public static OutletCommand fromOutletInfo(OutletInfo info, boolean enabled) {
        OutletCommand c = new OutletCommand();
        c.enabled = enabled;
        c.description = info.device.DeviceName + ": " + (info.UserDescription.isEmpty() ? info.Description : info.UserDescription);
        c.device_mac = info.device.MacAddress;
        c.outletNumber = info.OutletNumber;
        c.state = info.State ? 1 : 0;
        return c;
    }
}
