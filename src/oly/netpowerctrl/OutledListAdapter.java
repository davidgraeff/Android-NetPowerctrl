package oly.netpowerctrl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListAdapter;

public class OutledListAdapter extends BaseAdapter implements ListAdapter {
    private List<DeviceInfo> all_devices;
    private List<OutletInfo> all_outlets;
    private LayoutInflater inflater;
    final Activity context;
    
    public OutledListAdapter(Activity context, List<DeviceInfo> devices) {
    	this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfo>();
    	all_devices = devices;
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
    	}
    	OutletInfo info = all_outlets.get(position);      
    	TextView tv = (TextView) convertView.findViewById(R.id.outlet_list_item);
    	tv.setText(info.Description);
        return convertView;
    }
 
	public void update() {
    	all_outlets.clear();
    	for (DeviceInfo device: all_devices) {
        	for (OutletInfo oi: device.Outlets) {
        		oi.device = device;
        		oi.Disabled = false;
        		all_outlets.add(oi);
        	}
    	}
    	notifyDataSetChanged();
	}
}
