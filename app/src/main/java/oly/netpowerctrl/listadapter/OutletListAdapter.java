package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.ListItemMenu;

public class OutletListAdapter extends BaseAdapter implements ListAdapter, View.OnClickListener {
    private List<SceneOutlet> all_outlets;
    private List<Boolean> running = new ArrayList<Boolean>();
    private LayoutInflater inflater;
    private ArrayAdapter<CharSequence> spinner_adapter;
    private ListItemMenu listItemMenu = null;

    public static OutletListAdapter createByConfiguredDevices(Context context) {
        OutletListAdapter o = new OutletListAdapter(context);
        // Enumerate all configured devices and outlets and create SceneOutlets
        for (DeviceInfo device : NetpowerctrlApplication.instance.configuredDevices) {
            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                // Create SceneOutlet and set state to Toggle
                SceneOutlet so = SceneOutlet.fromOutletInfo(oi, false);
                so.state = SceneOutlet.TOGGLE;
                o.all_outlets.add(so);
                o.running.add(false);
            }
        }
        return o;
    }

    public static OutletListAdapter createByOutletCommands(Context context, List<SceneOutlet> commands) {
        OutletListAdapter o = new OutletListAdapter(context);
        for (DeviceInfo device : NetpowerctrlApplication.instance.configuredDevices) {
            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                SceneOutlet c = SceneOutlet.fromOutletInfo(oi, false);
                int i = commands.indexOf(c);
                if (i != -1) {
                    c.enabled = true;
                    c.state = commands.get(i).state;
                }
                o.all_outlets.add(c);
            }
        }
        return o;
    }

    private OutletListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<SceneOutlet>();

        spinner_adapter = ArrayAdapter.createFromResource(context, R.array.shortcutchoices, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
//            Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
//            r.setAdapter(spinner_adapter);
        }
        SceneOutlet command = all_outlets.get(position);
        TextView tv = (TextView) convertView.findViewById(R.id.outlet_list_text);
        if (command.enabled)
            tv.setPaintFlags(tv.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
        else
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tv.setOnClickListener(this);
        tv.setText(command.description);
        tv.setTag(position);

//        Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
//        r.setTag(position);
//        r.setSelection(command.state);
//        r.setEnabled(command.enabled);
//        r.setOnItemSelectedListener(this);
        View v;
        v = convertView.findViewById(R.id.radio0);
        v.setOnClickListener(radioClick);
        v.setTag(position);
        v.setVisibility(command.enabled ? View.VISIBLE : View.INVISIBLE);

        v = convertView.findViewById(R.id.radio1);
        v.setOnClickListener(radioClick);
        v.setTag(position);
        v.setVisibility(command.enabled ? View.VISIBLE : View.INVISIBLE);

        v = convertView.findViewById(R.id.radio2);
        v.setTag(position);
        v.setVisibility(command.enabled ? View.VISIBLE : View.INVISIBLE);

        RadioGroup r = (RadioGroup) convertView.findViewById(R.id.radioGroup);
        r.check((command.state == 0) ? R.id.radio0 : (command.state == 1) ? R.id.radio1 : R.id.radio2);
        return convertView;
    }

    public ArrayList<SceneOutlet> getCheckedItems() {
        ArrayList<SceneOutlet> output = new ArrayList<SceneOutlet>();
        for (SceneOutlet c : all_outlets) {
            if (c.enabled) {
                output.add(c);
            }
        }
        return output;
    }

    private View.OnClickListener radioClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            SceneOutlet info = all_outlets.get(position);
            int buttonId = view.getId();
            int sel = (buttonId == R.id.radio0) ? 0 : (buttonId == R.id.radio1) ? 1 : 2;
            Log.w("RADIO", "SEL" + Integer.valueOf(sel).toString());
            info.state = sel; //1:off;2:on;3:toggle
            if (listItemMenu != null)
                listItemMenu.onMenuItemClicked(view, position);
        }
    };

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    @Override
    public void onClick(View view) {
        int position = (Integer) view.getTag();
        SceneOutlet info = all_outlets.get(position);
        info.enabled = !info.enabled;
        notifyDataSetChanged();
        if (listItemMenu != null)
            listItemMenu.onMenuItemClicked(view, position);
    }

    public void toggleShowDeviceName() {
        //TODO toggleShowDeviceName
    }

    public void sortAlphabetically() {
        //TODO sortAlphabetically
    }

    public void switchAll(int state) {
        for (SceneOutlet so : all_outlets) {
            if (state == -1)
                so.enabled = false;
            else {
                so.enabled = true;
                so.state = state;
            }
        }
        notifyDataSetChanged();
        if (listItemMenu != null)
            listItemMenu.onMenuItemClicked(null, 0);
    }
}
