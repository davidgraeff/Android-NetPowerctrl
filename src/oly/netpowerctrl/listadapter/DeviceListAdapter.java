package oly.netpowerctrl.listadapter;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.DeviceConfigureEvent;
import oly.netpowerctrl.utils.DeviceInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter implements OnClickListener {

	private DeviceConfigureEvent deviceConfigureEvent = null;
	
    private List<DeviceInfo> all_devices;
    private LayoutInflater inflater;
    private Context context = null;
    
    public DeviceListAdapter(Context context, List<DeviceInfo> devices) {
    	this.context = context;
        inflater = LayoutInflater.from(context);
        all_devices = devices;
    }    

    public int getCount() {
        return all_devices.size();
    }

    public Object getItem(int position) {
        return all_devices.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public DeviceInfo findDevice(UUID uuid) {
    	for (DeviceInfo di: all_devices) {
    		if (di.equals(uuid)) {
    			return di;
    		}
    	}
    	return null;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {

    	if (convertView == null)
    		convertView = inflater.inflate(R.layout.device_list_item, null);
        
    	DeviceInfo di = all_devices.get(position);
        TextView tvName = (TextView) convertView.findViewById(R.id.device_name);
        tvName.setText(di.DeviceName);
        
        TextView tvIP = (TextView) convertView.findViewById(R.id.device_ip);
        if (di.isConfigured())
        	tvIP.setText(di.HostName);
        else
        	tvIP.setText(di.HostName + " (" + context.getResources().getString(R.string.default_device_name) + ")");
        
        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditDevice); 
        if (di.isConfigured())
        	btn.setImageResource(android.R.drawable.ic_menu_edit);
        else
        	btn.setImageResource(android.R.drawable.ic_menu_add);
        btn.setTag(position);
        btn.setFocusable(false); // or else onItemClick doesn't work in the ListView
        btn.setFocusableInTouchMode(false);
        btn.setOnClickListener(this);

        convertView.setTag(position);
        return convertView;
    }

    public void setDevices(List<DeviceInfo> new_devices) {
    	all_devices = new_devices;
    	update();
    }
    
    public void setDeviceConfigureEvent(DeviceConfigureEvent dce) {
    	deviceConfigureEvent = dce;
    }
    
	public void onClick(View v) {
		if (deviceConfigureEvent != null) {
			int position = (Integer)v.getTag();
			deviceConfigureEvent.onConfigureDevice(v, position);
		}
	}
    
	public void update() {
		notifyDataSetChanged();
	}

}
