package oly.netpowerctrl.main;

import java.util.ArrayList;
import java.util.Iterator;
import oly.netpowerctrl.R;
import oly.netpowerctrl.devicecontrol.DeviceControlActivity;
import oly.netpowerctrl.listadapter.DeviceListAdapter;
import oly.netpowerctrl.listadapter.GroupListAdapter;
import oly.netpowerctrl.listadapter.OutledSwitchListAdapter;
import oly.netpowerctrl.preferences.DevicePreferencesActivity;
import oly.netpowerctrl.preferences.PreferencesActivity;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.service.DiscoveryThread;
import oly.netpowerctrl.service.NetpowerctrlService;
import oly.netpowerctrl.service.ShortcutCreatorActivity;
import oly.netpowerctrl.utils.DeviceConfigureEvent;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.GreenFlasher;
import oly.netpowerctrl.utils.OutletCommandGroup;
import oly.netpowerctrl.utils.OutletInfo;
import oly.netpowerctrl.utils.SharedPrefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

@SuppressWarnings("deprecation")
public class NetpowerctrlActivity extends TabActivity implements OnItemClickListener, PopupMenu.OnMenuItemClickListener, DeviceConfigureEvent, OnTabChangeListener {
	final Activity _this = this;
	final static int ACTIVITY_REQUEST_ADDGROUP = 12; 
	
	ListView lvGroups;
	ListView lvAllOutlets;
	ListView lvDevices;
	
	ArrayList<DeviceInfo> alDevices;
	DeviceListAdapter adpDevices;
	OutledSwitchListAdapter adpOutlets;
	GroupListAdapter adpGroups;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        TabHost th = (TabHost)findViewById(android.R.id.tabhost);
        th.setup();
        th.addTab(th.newTabSpec("groups").setIndicator(getResources().getString(R.string.groups)).setContent(R.id.lvGroups));
        th.addTab(th.newTabSpec("outlets").setIndicator(getResources().getString(R.string.all_outlets)).setContent(R.id.lvAllOutlets));
        th.addTab(th.newTabSpec("devices").setIndicator(getResources().getString(R.string.devices)).setContent(R.id.lvDevices));
        th.setOnTabChangedListener(this);
        
        if (savedInstanceState != null) {
        	th.setCurrentTabByTag(savedInstanceState.getString("tab", "outlets"));
        } else {
        	th.setCurrentTabByTag(SharedPrefs.getFirstTab(this));
        }

  		lvDevices = (ListView)findViewById(R.id.lvDevices);
  		lvAllOutlets = (ListView)findViewById(R.id.lvAllOutlets);
  		lvGroups = (ListView)findViewById(R.id.lvGroups);
  		
    	alDevices = new ArrayList<DeviceInfo>();
    	adpDevices = new DeviceListAdapter(this, alDevices);
  		lvDevices.setAdapter(adpDevices);

        lvDevices.setOnItemClickListener(this);        
        adpDevices.setDeviceConfigureEvent(this);

  		adpOutlets = new OutledSwitchListAdapter(this, alDevices);
  		lvAllOutlets.setAdapter(adpOutlets);
  		
  		adpGroups = new GroupListAdapter(this);
  		lvGroups.setAdapter(adpGroups);

    }
    
    private void clearNonConfiguredDevices() {
    	alDevices = SharedPrefs.ReadConfiguredDevices(this);
    	adpDevices.setDevices(alDevices);
    	adpOutlets.setDevices(alDevices);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// we may be returning from a configure dialog
    	clearNonConfiguredDevices();
    	
    	IntentFilter itf= new IntentFilter(DiscoveryThread.BROADCAST_DEVICE_DISCOVERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onDeviceDiscovered, itf);
    	
		Intent it = new Intent(this, NetpowerctrlService.class);
		startService(it);
        
    	DeviceQuery.sendBroadcastQuery(this);
    }
    
    protected void onSaveInstanceState(Bundle icicle) {
    	super.onSaveInstanceState(icicle);
    	TabHost th = (TabHost)findViewById(android.R.id.tabhost);
    	icicle.putString("tab", th.getCurrentTabTag());
	}
    
    @Override
    protected void onPause() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(onDeviceDiscovered);
    	super.onPause();
    	TabHost th = (TabHost)findViewById(android.R.id.tabhost);
    	SharedPrefs.setFirstTab(this,th.getCurrentTabTag());
	}
    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
		menu.add(0, R.id.menu_add_device, 0, R.string.menu_add_device).setIcon(R.drawable.ic_menu_add);
		menu.add(0, R.id.menu_delete_all_devices, 0, R.string.menu_delete_all).setIcon(R.drawable.ic_menu_delete);
		menu.add(0, R.id.menu_requery, 0, R.string.requery).setIcon(R.drawable.ic_menu_refresh);
		menu.add(0, R.id.menu_preferences, 0, R.string.menu_preferences).setIcon(R.drawable.ic_menu_preferences);
		menu.add(0, R.id.menu_about, 0, R.string.menu_about).setIcon(R.drawable.ic_menu_info_details);
		menu.findItem(R.id.menu_add_device).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.findItem(R.id.menu_requery).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	TabHost th = (TabHost)findViewById(android.R.id.tabhost);
        menu.findItem(R.id.menu_add_device).setVisible(!th.getCurrentTabTag().equals("outlets"));
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_device: {
			TabHost th = (TabHost)findViewById(android.R.id.tabhost);
			if (th.getCurrentTabTag().equals("devices")) {
				DeviceInfo di = new DeviceInfo(this);
				di.setConfigured(true);
				alDevices.add(di);
				SaveConfiguredDevices(); // we need to know the device on return from config activity 
				Intent it = new Intent(this, DevicePreferencesActivity.class);
				it.putExtra("prefname", di.getPrefname());
				startActivity(it);
				return true;
			} else if (th.getCurrentTabTag().equals("groups")) {
				Intent it = new Intent(this, ShortcutCreatorActivity.class);
				startActivityForResult(it, ACTIVITY_REQUEST_ADDGROUP);
				return true;				
			}
			return false;
		}
		
		case R.id.menu_delete_all_devices: {
			new AlertDialog.Builder(this)
				.setTitle(R.string.delete_all_devices)
				.setMessage(R.string.confirmation_delete_all_devices)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
				    	deleteAllDevices();
				    }})
				 .setNegativeButton(android.R.string.no, null).show();
			return true;
		}
		
		case R.id.menu_requery: {
			clearNonConfiguredDevices();
	    	DeviceQuery.sendBroadcastQuery(this);
			return true;
		}
		
		case R.id.menu_preferences: {
			Intent it = new Intent(this, PreferencesActivity.class);
			startActivity(it);
			return true;
		}
		
		case R.id.menu_about: {
			AboutDialog about = new AboutDialog(this);
			about.setTitle(R.string.app_name);
			about.show();
			return true;
		}
		}
		return false;
	}

    public void SaveConfiguredDevices() {
    	ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>(alDevices);
    	for ( Iterator<DeviceInfo> i = configuredDevices.iterator(); i.hasNext(); )
			  if (!i.next().isConfigured())
				  i.remove();
    	
  		adpDevices.update();
  		adpOutlets.update();
  		
    	SharedPrefs.SaveConfiguredDevices(configuredDevices, this);
        DeviceQuery.restartDiscovery(this);  // ports may have changed
    }

  	@Override
  	public boolean onMenuItemClick(MenuItem item) {
  		final int position = (Integer)lvDevices.getTag();
  		DeviceInfo current_device = alDevices.get(position);
  		
  	    switch (item.getItemId()) {
  	    case R.id.menu_add_device: {
  	    	current_device.setConfigured(true);
  	    	SaveConfiguredDevices();
			Intent it = new Intent(this, DevicePreferencesActivity.class);
			it.putExtra("prefname", current_device.getPrefname());
			startActivity(it);
			return true;
  	    }
			
  	    case R.id.menu_configure_device: {
			Intent it = new Intent(this, DevicePreferencesActivity.class);
			it.putExtra("prefname", current_device.getPrefname());
			startActivity(it);
			return true;
  		}

  	    case R.id.menu_delete_device: {
  	    	if (!current_device.isConfigured())
  	    		return true;
  	    	
			new AlertDialog.Builder(this)
				.setTitle(R.string.delete_device)
				.setMessage(R.string.confirmation_delete_device)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
				    	deleteDevice(position);
				    }})
				 .setNegativeButton(android.R.string.no, null).show();
			return true;
  		}
  	    
  	    case R.id.menu_copy_device: {
  	    	if (!current_device.isConfigured())
  	    		return true;
  	    	
	  		DeviceInfo new_device = new DeviceInfo(current_device);
	  		new_device.setConfigured(true);
	  		new_device.DeviceName = String.format(getResources().getString(R.string.copy_of), new_device.DeviceName);
	  		alDevices.add(new_device);
	  		SaveConfiguredDevices();
	  		adpDevices.update();
	  		adpOutlets.update();
			return true;
  		}
  	    
  	    default:
  	    	return false;
  	    }
  	}

	public void onItemClick(AdapterView<?> av, View v, int position, long id) {
		Object o = av.getItemAtPosition(position);
		if (o != null) {
			DeviceInfo di = (DeviceInfo)o;
			if (di.isConfigured()) {
				Intent it = new Intent(this, DeviceControlActivity.class);
				it.putExtra("device", di);
				startActivity(it);				
			} else {
				onConfigureDevice(v, position);
			}
		}
	}

	public void onConfigureDevice(View v, int position) {

		lvDevices.setTag(position);
		PopupMenu popup = new PopupMenu(this, v);
	    MenuInflater inflater = popup.getMenuInflater();
		DeviceInfo current_device = alDevices.get(position);
		if (current_device.isConfigured()) {
			inflater.inflate(R.menu.configured_device_menu, popup.getMenu());
		} else {
			inflater.inflate(R.menu.unconfigured_device_menu, popup.getMenu());		
		}
		
		popup.setOnMenuItemClickListener(this);
	    popup.show();
		
	}    
	
	public void deleteDevice(int position) {
  		alDevices.remove(position);
  		SaveConfiguredDevices();
	}
	
	public void deleteAllDevices() {
  		alDevices.clear();
  		SaveConfiguredDevices();
	}

	private BroadcastReceiver onDeviceDiscovered= new BroadcastReceiver() {
	    @Override
	    synchronized public void onReceive(Context context, Intent intent) {
	    	DeviceInfo device_info = null;
			Bundle extra = intent.getExtras();
			if (extra != null) {
				Object o = extra.get("device_info");
				if (o != null) {
					device_info = (DeviceInfo) o; 
				}
			}
			if (device_info == null)
				return;

			// if it matches a configured device, update it's outlet states
			boolean found = false;
			for (DeviceInfo target: alDevices) {
				if (!device_info.MacAddress.equals(target.MacAddress))
					continue;
				for (OutletInfo srcoi: device_info.Outlets) {
					for (OutletInfo tgtoi: target.Outlets) {
						if (tgtoi.OutletNumber == srcoi.OutletNumber) {
							tgtoi.State = srcoi.State;
							break;
						}
					}
				}
				found = true;
				break;
			}
			
			if (!found) {
				// remove it's disabled outlets
				for ( Iterator<OutletInfo> i = device_info.Outlets.iterator(); i.hasNext(); )
				  if (i.next().Disabled)
					  i.remove();
				
				device_info.setConfigured(false);
				alDevices.add(device_info);
				adpDevices.update();
			}
			adpOutlets.update();
		
			// Highlight updated rows
			for (int i=0; i<lvDevices.getChildCount(); i++) {
				View child = lvDevices.getChildAt(i);
				if (child != null && adpDevices.getCount()>(Integer)child.getTag()) {
					DeviceInfo di = (DeviceInfo)adpDevices.getItem((Integer)child.getTag());
					if (di.MacAddress.equals(device_info.MacAddress)) {
						GreenFlasher.flashBgColor(child);
					}
				}	
			}
	    }
	};

	@Override
	public void onTabChanged(String tabId) {
		invalidateOptionsMenu();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_REQUEST_ADDGROUP && resultCode == RESULT_OK) {
			Bundle shortcut_bundle = data.getExtras();
			Intent groupIntent = shortcut_bundle.getParcelable(Intent.EXTRA_SHORTCUT_INTENT);
			shortcut_bundle = groupIntent.getExtras();
			OutletCommandGroup og = OutletCommandGroup.fromString(shortcut_bundle.getString("commands"), this);
			adpGroups.addGroup(og);
		}
	}
}