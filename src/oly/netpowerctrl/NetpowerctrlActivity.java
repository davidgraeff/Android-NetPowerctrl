package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.Toast;

public class NetpowerctrlActivity extends TabActivity implements OnItemClickListener {

	ListView lvConfiguredDevices;
	ListView lvDiscoveredDevices;
	
	ArrayList<Map<String, String>> alConfiguredDevices;
	ArrayList<Map<String, String>> alDiscoveredDevices;
	SimpleAdapter adpConfiguredDevices;
	SimpleAdapter adpDiscoveredDevices;
	
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

        lvConfiguredDevices.setOnItemClickListener(this);
        lvDiscoveredDevices.setOnItemClickListener(this);
        
        registerForContextMenu(lvConfiguredDevices);
        registerForContextMenu(lvDiscoveredDevices);
    }
    
    public void tmp() {
    	alConfiguredDevices = new ArrayList<Map<String,String>>();
    	adpConfiguredDevices = new SimpleAdapter(this,
    										     alConfiguredDevices,
    										     android.R.layout.simple_list_item_2,
    										     new String[]{ "name", "ip" },
    										     new int[] { android.R.id.text1, android.R.id.text2 });
  		lvConfiguredDevices.setAdapter(adpConfiguredDevices);

  		for (int i=0; i<4; i++) {
	  		HashMap<String, String> item = new HashMap<String, String>();
	  		item.put("name", String.format("conf %d", i));
	  		item.put("ip", String.format("c%d", i));
	  		alConfiguredDevices.add(item);
  		}
		
    	alDiscoveredDevices = new ArrayList<Map<String,String>>();
    	adpDiscoveredDevices = new SimpleAdapter(this,
    										     alDiscoveredDevices,
    										     android.R.layout.simple_list_item_2,
    										     new String[]{ "name", "ip" },
    										     new int[] { android.R.id.text1, android.R.id.text2 });
  		lvDiscoveredDevices.setAdapter(adpDiscoveredDevices);

  		for (int i=0; i<6; i++) {
	  		HashMap<String, String> item = new HashMap<String, String>();
	  		item.put("name", String.format("dis %d", i));
	  		item.put("ip", String.format("d%d", i));
	  		alDiscoveredDevices.add(item);
  		}
		
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