package oly.netpowerctrl.datastructure;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;

import oly.netpowerctrl.R;


public class OutletCommandGroup {
    public String sceneName = "";
    public String sceneDetails = "";
    private String reserved = "";
    private UUID uuid;
    public ArrayList<OutletCommand> commands;

    public OutletCommandGroup() {
        uuid = UUID.randomUUID();
        commands = new ArrayList<OutletCommand>();
    }

    @SuppressWarnings("unused")
    public boolean equals(OutletCommandGroup other) {
        return uuid.equals(other.uuid);
    }

    @SuppressWarnings("unused")
    public boolean equals(UUID uuid) {
        return uuid.equals(uuid);
    }

    public String toString() {
        String result = sceneName.replace("§§", "") + "§§" + reserved.replace("§§", "") + "§§" + uuid.toString();
        for (OutletCommand c : commands) {
            result += "§§" + c.toString();
        }
        return result;
    }

    public static OutletCommandGroup fromString(String source, Context context) {
        if (source == null)
            return null;

        OutletCommandGroup og = new OutletCommandGroup();
        String list_src[] = source.split("§§");
        if (list_src.length == 0)
            return null;

        // sceneName is the first element, uuid the third
        og.sceneName = list_src[0];
        og.reserved = list_src[1];
        og.uuid = UUID.fromString(list_src[2]);

        for (int i = 3; i < list_src.length; ++i) {
            OutletCommand c = OutletCommand.fromString(list_src[i]);
            if (c != null)
                og.commands.add(c);
        }

        og.sceneDetails = og.buildDetails(context);

        return og;
    }

    public String buildDetails(Context context) {
        int ons = 0;
        int offs = 0;
        int toggles = 0;
        for (OutletCommand c : commands) {
            switch (c.state) {
                case 0:
                    ++offs;
                    break;
                case 1:
                    ++ons;
                    break;
                case 2:
                    ++toggles;
                    break;
            }
        }
        return context.getResources().getString(R.string.off) + ": " + Integer.valueOf(offs) + ", " +
                context.getResources().getString(R.string.on) + ": " + Integer.valueOf(ons) + ", " +
                context.getResources().getString(R.string.toggle) + ": " + Integer.valueOf(toggles);
    }

    public void add(OutletCommand c) {
        commands.add(c);
    }

    public int length() {
        return commands.size();
    }

    public Collection<DeviceInfo> getDevices() {
        TreeMap<String, DeviceInfo> devices = new TreeMap<String, DeviceInfo>();
        for (OutletCommand c : commands) {
            if (!devices.containsKey(c.device_mac)) {
                devices.put(c.device_mac, c.outletinfo.device);
            }
        }
        return devices.values();
    }
}
