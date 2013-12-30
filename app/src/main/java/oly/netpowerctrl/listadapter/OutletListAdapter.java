package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommand;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.service.DeviceUpdated;
import oly.netpowerctrl.utils.ListItemMenu;

public class OutletListAdapter extends BaseAdapter implements ListAdapter, OnItemSelectedListener, DeviceUpdated {
    private List<OutletCommand> all_outlets;
    private LayoutInflater inflater;
    private ArrayAdapter<CharSequence> spinner_adapter;
    private ListItemMenu listItemMenu = null;

    public OutletListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletCommand>();
        NetpowerctrlApplication.instance.registerConfiguredObserver(this);

        spinner_adapter = ArrayAdapter.createFromResource(context, R.array.shortcutchoices, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        update();
    }

    public int getCount() {
        return all_outlets.size();
    }

    public Object getItem(int position) {
        return all_outlets.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.outlet_list_item, null);
            assert convertView != null;
            Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
            r.setAdapter(spinner_adapter);
        }
        OutletCommand command = all_outlets.get(position);
        TextView tv = (TextView) convertView.findViewById(R.id.outlet_list_text);
        tv.setText(command.description);
        tv.setTag(position);

        Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
        r.setTag(position);
        if (!command.enabled)
            r.setSelection(0);
        else
            r.setSelection(command.state + 1);
        r.setOnItemSelectedListener(this);

        return convertView;
    }

    void update() {
        all_outlets.clear();
        for (DeviceInfo device : NetpowerctrlApplication.instance.configuredDevices) {
            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                all_outlets.add(OutletCommand.fromOutletInfo(oi, false));
            }
        }
        notifyDataSetChanged();
    }

    public OutletCommandGroup getCheckedItems() {
        OutletCommandGroup og = new OutletCommandGroup();
        for (OutletCommand c : all_outlets) {
            if (c.enabled) {
                og.add(c);
            }
        }
        return og;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner sp = (Spinner) parent;
        OutletCommand info = all_outlets.get((Integer) parent.getTag());
        int sel = sp.getSelectedItemPosition();
        info.enabled = (!(sel == 0 || sel == Spinner.INVALID_POSITION));
        if (info.enabled) {
            info.state = sel - 1; //1:off;2:on;3:toggle
        }
        if (listItemMenu != null)
            listItemMenu.onMenuItemClicked(view, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        update();
    }
}
