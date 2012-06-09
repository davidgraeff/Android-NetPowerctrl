package oly.netpowerctrl;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

public class NetpowerctrlActivity extends TabActivity implements OnItemClickListener {

	ListView lvConfiguredDevices;
	ListView lvDiscoveredDevices;
	
	ArrayList<DeviceInfo> alConfiguredDevices;
	ArrayList<DeviceInfo> alDiscoveredDevices;
	DeviceListAdapter adpConfiguredDevices;
	DeviceListAdapter adpDiscoveredDevices;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TabHost th = (TabHost)findViewById(android.R.id.tabhost);
        th.setup();
        th.addTab(th.newTabSpec("conf").setIndicator(getResources().getString(R.string.configured_devices)).setContent(R.id.lvConfiguredDevices));
        th.addTab(th.newTabSpec("found").setIndicator(getResources().getString(R.string.discovered_devices)).setContent(R.id.lvDiscoveredDevices));

  		lvConfiguredDevices = (ListView)findViewById(R.id.lvConfiguredDevices);
  		lvDiscoveredDevices = (ListView)findViewById(R.id.lvDiscoveredDevices);

    	alConfiguredDevices = new ArrayList<DeviceInfo>();
    	adpConfiguredDevices = new DeviceListAdapter(this, alConfiguredDevices);
  		lvConfiguredDevices.setAdapter(adpConfiguredDevices);

  		tmp();
        ReadConfiguredDevices();

        lvConfiguredDevices.setOnItemClickListener(this);
        lvDiscoveredDevices.setOnItemClickListener(this);
        
        registerForContextMenu(lvConfiguredDevices);
        registerForContextMenu(lvDiscoveredDevices);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.id.menu_add_device, 0, R.string.menu_add_device).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, R.id.menu_about, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_device:
			Intent it = new Intent(this, DevicePreferences.class);
			it.putExtra("new_device", true);
			it.putExtra("device_info", new DeviceInfo(this));
			startActivityForResult(it, R.id.request_code_new_device);
			return true;

		case R.id.menu_about:
			AboutDialog about = new AboutDialog(this);
			about.setTitle(R.string.app_name);
			about.show();
			return true;
		}
		return false;
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (requestCode == R.id.request_code_cancel)
			return;
		
        DeviceInfo device_info = (DeviceInfo) data.getExtras().get("device_info");
        SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
    	String device_name = prefs.getString("setting_device_name", "ERROR");
    	String device_ip = prefs.getString("setting_device_ip", "");
    	boolean standard_ports = prefs.getBoolean("setting_standard_ports", false);
    	int send_udp = prefs.getInt("setting_send_udp", R.integer.default_send_port);
    	int recv_udp = prefs.getInt("setting_recv_udp", R.integer.default_recv_port);
		String username = prefs.getString("setting_username", "");
		String password = prefs.getString("setting_password", "");
        
		if (requestCode == R.id.request_code_new_device) {
			if ((device_name == "") &&
				(device_ip == "") &&
				(username == "") &&
				(password == "")) {
				// editing was cancelled by user
				return;
			}
			
			device_info.DeviceName = device_name;
			device_info.HostName = device_ip;
			device_info.UserName = username;
			device_info.Password = password;
			if (standard_ports) {
				device_info.SendPort = R.integer.default_send_port;
				device_info.RecvPort = R.integer.default_recv_port;
			} else {
				device_info.SendPort = send_udp;
				device_info.RecvPort = recv_udp;
			}
			alConfiguredDevices.add(device_info);
	        adpConfiguredDevices.getFilter().filter("");
		}
		
	}
	
    public void tmp() {
		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("configured_devices", "[ {'name': 'o1', 'ip': '123', 'outlets': [{'description': 'x'},{'description': 'y'}]}, {'name': 'o2', 'ip': '192.168.178.7', 'outlets': [{'description': 'x'},{'description': 'y'}]} ]");
		prefEditor.commit();
	}
    
    public void ReadConfiguredDevices() {
    	alConfiguredDevices.clear();

		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		String configured_devices_str = prefs.getString("configured_devices", "[]");
  		try {
			JSONArray jdevices = new JSONArray(configured_devices_str);
			
			for (int i=0; i<jdevices.length(); i++) {
				JSONObject jhost = jdevices.getJSONObject(i);
				DeviceInfo di = new DeviceInfo(this);
				di.DeviceName = jhost.getString("name");
				di.HostName = jhost.getString("ip");
				di.Outlets = new ArrayList<OutletInfo>();

				JSONArray joutlets = jhost.getJSONArray("outlets");
				for (int j=0; j<joutlets.length(); j++) {
					JSONObject joutlet = joutlets.getJSONObject(j);
					OutletInfo oi = new OutletInfo();
					oi.Description = joutlet.getString("description");
					di.Outlets.add(oi);
				}

				alConfiguredDevices.add(di);
			}
		}
		catch (JSONException e) {
			Toast.makeText(getBaseContext(), getResources().getText(R.string.error_reading_configured_devices) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
        adpConfiguredDevices.getFilter().filter("");
    }
    
  	@Override
  	public void onCreateContextMenu(ContextMenu cm, View v, ContextMenuInfo cmi) {
  		super.onCreateContextMenu(cm, v, cmi);
  	    MenuInflater inflater = getMenuInflater();
  	    
  	    if (v == lvConfiguredDevices)
  	    	inflater.inflate(R.menu.configured_device_menu, cm);
  	    else
  	    	inflater.inflate(R.menu.discovered_device_menu, cm);

  	}

  	@Override
  	public boolean onContextItemSelected(MenuItem item) {
  		/*
  	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			Object o = hosts_adapter.getItem(info.position);
			@SuppressWarnings("unchecked")
		Map<String,String> map = (Map<String,String>)o;
  	    switch (item.getItemId()) {
  	        case R.id.menu_edit_host:
  	        	Toast.makeText(getBaseContext(), String.format("Edit %s/%s", map.get("name"), map.get("ip")), Toast.LENGTH_LONG).show();
  	            return true;
  	        case R.id.menu_delete_host:
  	        	host_list.remove(info.position);
  	        	hosts_adapter.notifyDataSetChanged();
  	            return true;
  	        default:
  	            return super.onContextItemSelected(item);
  	    }
  	    */
  		return super.onContextItemSelected(item);
  	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long id) {
		Object o = av.getItemAtPosition(position);
		if (o != null) {
			DeviceInfo di = (DeviceInfo)o;
			Toast.makeText(getBaseContext(), String.format("click %s/%s", di.DeviceName, di.HostName), Toast.LENGTH_LONG).show();
		}
	}     
}