package oly.netpowerctrl.datastructure;

import android.content.Context;

import java.util.ArrayList;
import java.util.UUID;

import oly.netpowerctrl.R;


public class OutletCommandGroup {
    public String groupname;
    public String groupdetails;
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
        String result = groupname.replace("§§", "") + "§§" + reserved.replace("§§", "") + "§§" + uuid.toString();
        for (OutletCommand c : commands) {
            result += "§§" + c.toString();
        }
        return result;
    }

    public static OutletCommandGroup fromString(String source, Context context) {
        if (source == null)
            return null;

        OutletCommandGroup og = new OutletCommandGroup();
        String listsrc[] = source.split("§§");
        if (listsrc.length == 0)
            return null;

        // groupname is the first element, uuid the third
        og.groupname = listsrc[0];
        og.reserved = listsrc[1];
        og.uuid = UUID.fromString(listsrc[2]);

        for (int i = 3; i < listsrc.length; ++i) {
            OutletCommand c = OutletCommand.fromString(listsrc[i]);
            if (c != null)
                og.commands.add(c);
        }

        og.groupdetails = og.buildDetails(context);

        return og;
    }

    public String buildDetails(Context context) {
        String r;
        String all = "";
        for (OutletCommand c : commands) {
            switch (c.state) {
                case 0:
                    r = context.getResources().getString(R.string.off);
                    break;
                case 1:
                    r = context.getResources().getString(R.string.on);
                    break;
                case 2:
                    r = context.getResources().getString(R.string.toggle);
                    break;
                default:
                    r = "";
            }
            all += c.description + " (" + r + ") ";
        }
        return all;
    }

    public void add(OutletCommand c) {
        commands.add(c);
    }

    public int length() {
        return commands.size();
    }
}
