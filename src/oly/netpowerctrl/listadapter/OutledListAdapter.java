package oly.netpowerctrl.listadapter;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.OutletCommand;
import oly.netpowerctrl.utils.OutletInfo;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ListAdapter;

public class OutledListAdapter extends BaseAdapter implements ListAdapter, OnItemSelectedListener {
    private List<DeviceInfo> all_devices;
    private List<OutletCommand> all_outlets;
    private LayoutInflater inflater;
    final Activity context;
    ArrayAdapter<CharSequence> spinneradapter;
    
    public OutledListAdapter(Activity context, List<DeviceInfo> devices) {
    	this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletCommand>();
    	all_devices = devices;
    	
    	spinneradapter = ArrayAdapter.createFromResource(context, R.array.shortcutchoices, android.R.layout.simple_spinner_item);
    	spinneradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
    		Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
    		r.setAdapter(spinneradapter);
    	}
    	OutletCommand command = all_outlets.get(position);
    	TextView tv = (TextView) convertView.findViewById(R.id.outlet_list_text);
    	tv.setText(command.Description);
    	tv.setTag(position);
    	
    	Spinner r = (Spinner) convertView.findViewById(R.id.outlet_list_spinner);
    	r.setTag(position);
    	if (!command.enabled)
    		r.setSelection(0);
    	else
    		r.setSelection(command.state+1);
    	r.setOnItemSelectedListener(this);
    	
        return convertView;
    }
 
	public void update() {
    	all_outlets.clear();
    	for (DeviceInfo device: all_devices) {
        	for (OutletInfo oi: device.Outlets) {
        		oi.device = device;
        		all_outlets.add(OutletCommand.fromOutletInfo(oi, false));
        	}
    	}
    	notifyDataSetChanged();
	}

	public List<OutletCommand> getCheckedItems() {
		ArrayList<OutletCommand> checked_outlets = new ArrayList<OutletCommand>();
		for (OutletCommand c: all_outlets) {
			if (c.enabled) {
				checked_outlets.add(c);
			}
		}
		return checked_outlets;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Spinner sp= (Spinner)parent;
		OutletCommand info = all_outlets.get((Integer)parent.getTag());
		int sel = sp.getSelectedItemPosition();
		info.enabled = (sel==0 || sel==Spinner.INVALID_POSITION) ? false : true;
		if (info.enabled) {
			info.state = sel-1; //1:off;2:on;3:toggle
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
}
