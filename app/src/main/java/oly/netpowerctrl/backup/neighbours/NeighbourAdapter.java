package oly.netpowerctrl.backup.neighbours;

import android.annotation.SuppressLint;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Adapter showing all detected neighbours. It also loads and saves paired neighbours.
 */
public class NeighbourAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<AdapterItem> items = new ArrayList<>();

    public NeighbourAdapter() {
        // Load
        String json = SharedPrefs.loadNeighbours();
        if (json == null)
            return;

        JsonReader reader = JSONHelper.getReader(json);
        if (reader == null)
            return;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                assert name != null;
                if (name.equals("items")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        readAdapterItem(reader);
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPaired(AdapterItem item, boolean paired) {
        item.isPaired = paired;
        item.updateData();
        save();
        notifyDataSetChanged();
    }

    public void removePairing(long uniqueID) {
        for (AdapterItem item : items) {
            if (item.uniqueID == uniqueID) {
                item.isPaired = false;
                item.updateData();
                save();
                notifyDataSetChanged();
                return;
            }
        }
    }

    private void save() {
        SharedPrefs.saveNeighbours(toJSON());
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    /**
     * Return the json representation of all groups
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        return toJSON();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("items").beginArray();
        for (AdapterItem c : items) {
            if (!c.isPaired)
                continue;
            writer.beginObject();
            writer.name("name").value(c.name);
            writer.name("version").value(c.version);
            writer.name("versionCode").value(c.versionCode);
            writer.name("devices").value(c.devices);
            writer.name("scenes").value(c.scenes);
            writer.name("icons").value(c.icons);
            writer.name("versionCode").value(c.versionCode);
            writer.name("uniqueID").value(c.uniqueID);
            writer.name("address").value(c.address.getHostAddress());
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    private void readAdapterItem(JsonReader reader) throws IOException {
        AdapterItem item = new AdapterItem();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "name":
                    item.name = reader.nextString();
                    break;
                case "version":
                    item.version = reader.nextInt();
                    break;
                case "versionCode":
                    item.versionCode = reader.nextInt();
                    break;
                case "groups":
                    item.groups = (short) reader.nextInt();
                    break;
                case "scenes":
                    item.scenes = (short) reader.nextInt();
                    break;
                case "devices":
                    item.devices = (short) reader.nextInt();
                    break;
                case "icons":
                    item.icons = (short) reader.nextInt();
                    break;
                case "uniqueID":
                    item.uniqueID = reader.nextLong();
                    break;
                case "address":
                    item.address = InetAddress.getByName(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        item.updateData();
        items.add(item);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public AdapterItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
        }

        AdapterItem item = items.get(i);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(android.R.id.text1);
        tvName.setText(item.name);

        TextView tvData = (TextView) convertView.findViewById(android.R.id.text2);
        tvData.setText(item.data);

        return convertView;
    }

    @SuppressLint("StringFormatMatches")
    public void add(String name, long uniqueID, int version, int versionCode,
                    short devices, short scenes, short groups, short icons,
                    InetAddress address) {

        int position = -1;
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).uniqueID == uniqueID) {
                position = i;
                break;
            }

        if (position != -1) {
            AdapterItem item = items.get(position);
            item.name = name;
            item.version = version;
            item.versionCode = versionCode;
            item.devices = devices;
            item.scenes = scenes;
            item.groups = groups;
            item.icons = icons;
            item.times = 0;
            item.updateData();
            item.isOnline = true;
        } else {
            AdapterItem item = new AdapterItem(name, version, versionCode, uniqueID, address,
                    devices, scenes, groups, icons);
            item.updateData();
            items.add(item);
        }
        notifyDataSetChanged();
    }

    public AdapterItem getItemByID(long uniqueID) {
        int position = -1;
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).uniqueID == uniqueID) {
                position = i;
                break;
            }
        if (position != -1) {
            return items.get(position);
        }
        return null;
    }

    public void advanceTime() {
        boolean changed = false;
        Iterator<AdapterItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            AdapterItem item = itemIterator.next();
            if (++item.times > 1) {
                if (!item.isPaired)
                    itemIterator.remove();
                item.isOnline = false;
                item.updateData();
                changed = true;
            }
        }
        if (changed)
            notifyDataSetChanged();
    }

    public AdapterItem getNeighbour(int position) {
        return items.get(position);
    }

    public static class AdapterItem {
        public boolean isSameVersion;
        public InetAddress address;
        public boolean isPaired;
        public boolean isOnline;
        public boolean pairingRequest = false;
        public int version;
        public int versionCode;
        public short devices;
        public short scenes;
        public short groups;
        public short icons;
        long uniqueID;
        private String data;
        private int times = 0;
        private String name;

        /**
         * Use this constructor for loaded, paired neighbours
         */
        AdapterItem() {
            this.isPaired = true;
            this.isOnline = false;
        }

        /**
         * Use this constructor for discovered neighbours who are not paired.
         */
        private AdapterItem(String name, int version, int versionCode,
                            long uniqueID, InetAddress address,
                            short devices, short scenes, short groups, short icons) {
            this.name = name;
            this.version = version;
            this.versionCode = versionCode;
            this.uniqueID = uniqueID;
            this.address = address;
            this.devices = devices;
            this.scenes = scenes;
            this.groups = groups;
            this.icons = icons;

            this.isPaired = false;
            this.isOnline = true;
        }

        public String getName() {
            return name;
        }

        public void updateData() {
            data = String.valueOf(uniqueID) + ": ";
            if (version < versionCode)
                data += NetpowerctrlApplication.instance.getString(R.string.neighbour_older_version) + " ";
            else if (version > versionCode)
                data += NetpowerctrlApplication.instance.getString(R.string.neighbour_newer_version) + " ";

            data += NetpowerctrlApplication.instance.getString(R.string.neighbour_entry, devices, scenes, groups, icons);
            if (isPaired)
                data += ", " + NetpowerctrlApplication.instance.getString(R.string.neighbour_paired);
            if (!isOnline)
                data += ", " + NetpowerctrlApplication.instance.getString(R.string.neighbour_paired_not_found);

            this.isSameVersion = versionCode == version;
        }
    }
}
