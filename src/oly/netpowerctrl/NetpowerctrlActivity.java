package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
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

  		tmp();
        ReadConfiguredDevices();

        lvConfiguredDevices.setOnItemClickListener(this);
        lvDiscoveredDevices.setOnItemClickListener(this);
        
        registerForContextMenu(lvConfiguredDevices);
        registerForContextMenu(lvDiscoveredDevices);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.id.id_menu_about, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.id_menu_about:
			AboutDialog about = new AboutDialog(this);
			about.setTitle(R.string.app_name);
			about.show();
			return true;
		/*
		case MENU_ABOUT:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
			*/
		}
		return false;
	}
	
    public void tmp() {
		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString("configured_devices", "[ {'name': 'o1', 'ip': '123', 'outlets': [{'description': 'x'},{'description': 'y'}]}, {'name': 'o2', 'ip': '192.168.178.7', 'outlets': [{'description': 'x'},{'description': 'y'}]} ]");
		prefEditor.commit();
	}
    
    public void ReadConfiguredDevices() {
    	alConfiguredDevices = new ArrayList<DeviceInfo>();
    	adpConfiguredDevices = new DeviceListAdapter(this, alConfiguredDevices);
  		lvConfiguredDevices.setAdapter(adpConfiguredDevices);

		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		String configured_devices_str = prefs.getString("configured_devices", "[]");
  		try {
			JSONArray jdevices = new JSONArray(configured_devices_str);
			
			for (int i=0; i<jdevices.length(); i++) {
				JSONObject jhost = jdevices.getJSONObject(i);
				DeviceInfo di = new DeviceInfo();
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
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Object o = arg0.getItemAtPosition(position);
		if (o != null) {
			@SuppressWarnings("unchecked")
			Map<String,String> map = (Map<String,String>)o;
			Toast.makeText(getBaseContext(), String.format("click %s/%s", map.get("name"), map.get("ip")), Toast.LENGTH_LONG).show();
		}
	}     
}