package oly.netpowerctrl.transfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Created by david on 05.05.14.
 */
public class NeighbourAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private List<AdapterItem> items = new ArrayList<AdapterItem>();

    public void setPaired(AdapterItem item, boolean paired) {
        item.isPaired = paired;
        notifyDataSetChanged();
    }

    public static class AdapterItem {
        private String data;
        public boolean isSameVersion;
        private int times = 0;
        private long uniqueID;
        public InetAddress address;
        public boolean isPaired;
        public boolean pairingRequest = false;
        private String name;

        private AdapterItem(String name, String data, boolean isSameVersion, long uniqueID, InetAddress address, boolean paired) {
            this.name = name;
            this.data = data;
            this.isSameVersion = isSameVersion;
            this.uniqueID = uniqueID;
            this.address = address;
            this.isPaired = paired;
        }

        public String getName() {
            return name;
        }
    }


    public NeighbourAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(android.R.id.text1);
        tvName.setText(items.get(i).data);

        return convertView;
    }

    public void add(String name, long uniqueID, int version, int versionCode,
                    short devices, short scenes, short groups, short icons,
                    InetAddress address, boolean paired) {

        String s = name + " (" + String.valueOf(uniqueID) + ") ";
        if (version < versionCode)
            s += NetpowerctrlApplication.instance.getString(R.string.neighbour_older_version) + "\n";
        else if (version > versionCode)
            s += NetpowerctrlApplication.instance.getString(R.string.neighbour_newer_version) + "\n";
        else
            s += "\n";

        s += NetpowerctrlApplication.instance.getString(R.string.neighbour_entry, devices, scenes, groups, icons);

        int position = -1;
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).uniqueID == uniqueID) {
                position = i;
                break;
            }

        if (position != -1) {
            AdapterItem item = items.get(position);
            item.data = s;
            item.times = 0;
        } else {
            items.add(new AdapterItem(name, s, version == versionCode, uniqueID, address, paired));
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
                itemIterator.remove();
                changed = true;
            }
        }
        if (changed)
            notifyDataSetChanged();
    }

    public AdapterItem getNeighbour(int position) {
        return items.get(position);
    }
}
