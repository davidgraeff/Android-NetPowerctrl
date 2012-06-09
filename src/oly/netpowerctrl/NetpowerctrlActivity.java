package oly.netpowerctrl;

import java.util.ArrayList;

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
		menu.add(0, R.id.id_menu_add_device, 0, R.string.menu_add_device).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, R.id.id_menu_about, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.id_menu_add_device:
			int devno = alConfiguredDevices.size();
			Intent it = new Intent(this, DevicePreferences.class);
			it.putExtra("new_device", true);
			//it.putExtra("device_info", new DeviceInfo());
			startActivityForResult(it, devno);
			return true;

		case R.id.id_menu_about:
			AboutDialog about = new AboutDialog(this);
			about.setTitle(R.string.app_name);
			about.show();
			return true;
		}
		return false;
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		Toast.makeText(getBaseContext(), String.format("result %d", resultCode), Toast.LENGTH_LONG).show();
		ReadConfiguredDevices();
	}
	
    public void tmp() {
		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putInt("num_configured_devices", 2);
		
		prefEditor.putString("setting_device_name_0", "gaga");
		prefEditor.putString("setting_device_ip_0", "lala");
		prefEditor.putBoolean("setting_standard_ports_0", true);
		
		prefEditor.putString("setting_device_name_1", "lulu");
		prefEditor.putString("setting_device_ip_1", "schuhu");
		prefEditor.putBoolean("setting_standard_ports_1", false);
		prefEditor.putInt("setting_send_udp_1",17);
		prefEditor.putInt("setting_recv_udp_1",18);
		
		prefEditor.commit();
	}
    
    public void ReadConfiguredDevices() {
    	alConfiguredDevices.clear();

		SharedPreferences prefs = getSharedPreferences("oly.netpowerctrl", MODE_PRIVATE);
		int num_configured_devices = prefs.getInt("num_configured_devices", 0);
		for (int i=0; i<num_configured_devices; i++) {
			DeviceInfo di = new DeviceInfo();
			di.DeviceName = prefs.getString(String.format("setting_device_name_%d", i), "unknown device");
			di.HostName = prefs.getString(String.format("setting_device_ip_%d", i), "unknown device");
			di.Outlets = new ArrayList<OutletInfo>();

			alConfiguredDevices.add(di);
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