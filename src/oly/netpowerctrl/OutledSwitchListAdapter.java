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
import android.widget.Toast;
import android.widget.ListAdapter;

public class OutledSwitchListAdapter extends BaseAdapter implements ListAdapter, OnCheckedChangeListener {
    private List<DeviceInfo> all_devices;
    private List<OutletInfo> all_outlets;
    private LayoutInflater inflater;
    final Activity context;
    AfterSentHandler ash = new AfterSentHandler(this);
    
    public OutledSwitchListAdapter(Activity context, List<DeviceInfo> devices) {
    	this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfo>();
        setDevices(devices);
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
    		convertView = inflater.inflate(R.layout.outlet_list_switch, null);
    		Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
            tv.setOnCheckedChangeListener(this);    		
    	}
    	OutletInfo info = all_outlets.get(position);      
    	Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
    	tv.setTag(-1);
    	tv.setText(info.Description);
    	tv.setChecked(info.State);
    	tv.setEnabled(!info.Disabled);
    	tv.setTag(position);
        return convertView;
    }

    public void setDevices(List<DeviceInfo> new_devices) {
    	all_devices = new_devices;
    	update();
    }
 
	public void update() {
    	ash.removeMessages();
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

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean new_state) {
		int position = (Integer)arg0.getTag();
		if (position==-1)
			return;
		arg0.setEnabled(false);
		OutletInfo oi = (OutletInfo) getItem(position);
		oi.Disabled = false;
		ash.setData(position, new_state);
		ash.removeMessages();
		ash.startDelayedCheck();
		UDPSendToDevice.sendOutlet(context, oi.device, oi.OutletNumber, new_state);
	}
}
