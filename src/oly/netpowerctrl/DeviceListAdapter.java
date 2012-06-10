package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter implements Filterable, OnClickListener {

	private DeviceConfigureEvent deviceConfigureEvent = null;
	
    private List<DeviceInfo> all_devices;
    private List<DeviceInfo> visible_devices;
    private LayoutInflater inflater;
    private DeviceFilter filter = null;

    public DeviceListAdapter(Context context, List<DeviceInfo> devices) {
        inflater = LayoutInflater.from(context);
        this.all_devices = devices;
        visible_devices = new ArrayList<DeviceInfo>(devices);
    }    

    @Override
    public int getCount() {
        return visible_devices.size();
    }

    @Override
    public Object getItem(int position) {
        return visible_devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

    	convertView = inflater.inflate(R.layout.device_list_item, null);
        
        TextView tvName = (TextView) convertView.findViewById(R.id.device_name);
        tvName.setText(visible_devices.get(position).DeviceName);
        
        TextView tvIP = (TextView) convertView.findViewById(R.id.device_ip);
        tvIP.setText(visible_devices.get(position).HostName);
        
        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditDevice); 
        btn.setTag(position);
        btn.setFocusable(false); // or else onItemClick doesn't work in the ListView
        btn.setFocusableInTouchMode(false);
        btn.setOnClickListener(this);

        return convertView;
    }

    public void setDeviceConfigureEvent(DeviceConfigureEvent dce) {
    	deviceConfigureEvent = dce;
    }
    
	@Override
	public void onClick(View v) {
		if (deviceConfigureEvent != null) 
			deviceConfigureEvent.onConfigureDevice((Integer)v.getTag());
	}
    
    
	@Override
	public Filter getFilter() {
	   if (filter == null) {
            filter = new DeviceFilter();
        }
        return filter;
    }
	
	private class DeviceFilter extends Filter
	{

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
            ArrayList<DeviceInfo> list;

			if (constraint == null || constraint.length() == 0) {
                list = new ArrayList<DeviceInfo>(all_devices);
			} else {
                list = new ArrayList<DeviceInfo>();
				String match = constraint.toString().toLowerCase();
				for (DeviceInfo item : all_devices) {
					if (item.DeviceName.toLowerCase().contains(match))
						list.add(item);
				}
				
			}
            results.values = list;
            results.count = list.size();
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			visible_devices = (List<DeviceInfo>)results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
		}
		
	}

}
