package oly.netpowerctrl;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;


public class DevicePreferences extends PreferenceActivity {

	public DeviceInfo device_info;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean new_device = getIntent().getExtras().getBoolean("new_device");
        Intent ret_intent = new Intent(); // for returning data to onActivityResult()
        ret_intent.putExtra("SharedPreferencesName", getPreferenceManager().getSharedPreferencesName());
        setResult(RESULT_OK, ret_intent); // default
        
        SharedPreferences pref = getSharedPreferences(getPreferenceManager().getSharedPreferencesName(), MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();

        if (new_device) 
        	device_info = new DeviceInfo(this);
        	else  device_info = (DeviceInfo) getIntent().getExtras().get("device_info");
        
        ret_intent.putExtra("uuid", device_info.uuid);
    	setTitle(device_info.DeviceName);
    	prefEditor.putString("setting_device_name", device_info.DeviceName);
		prefEditor.putString("setting_device_ip", device_info.HostName);
		prefEditor.putString("setting_device_mac", device_info.MacAddress);
		prefEditor.putBoolean("setting_nonstandard_ports", ! device_info.DefaultPorts);
		prefEditor.putString("setting_send_udp", String.format("%d",device_info.SendPort));
		prefEditor.putString("setting_recv_udp", String.format("%d",device_info.RecvPort));
		prefEditor.putString("setting_username", device_info.UserName);
		prefEditor.putString("setting_password", device_info.Password);
		prefEditor.commit();
		
    	save_outlet_settings(device_info);

        addPreferencesFromResource(R.xml.device_preferences);
		
		Preference p = findPreference("setting_device_name");
		p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setTitle(newValue.toString());
				return true;
			}
		});
		
		Preference myPref = (Preference) findPreference("setting_outlets");
		final Activity self = this;
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent it = new Intent(self, OutletConfig.class);
				it.putExtra("device_info", device_info);
				startActivityForResult(it, R.id.request_code_config_outlets);
			    return true;
			}
		});

    }
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED)
			return;
		
		if (requestCode == R.id.request_code_config_outlets) {
        	device_info = (DeviceInfo) data.getExtras().get("device_info");
        	save_outlet_settings(device_info);
		}
	}
	
	public void save_outlet_settings(DeviceInfo device_info) {
    	SharedPreferences pref = getSharedPreferences(getPreferenceManager().getSharedPreferencesName(), MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putInt("num_outlets", device_info.Outlets.size());
        for (int i=0; i<device_info.Outlets.size(); i++) {
        	prefEditor.putString(String.format("outlet_name%d",i), device_info.Outlets.get(i).Description);
        	prefEditor.putInt(String.format("outlet_number%d",i), device_info.Outlets.get(i).OutletNumber);
        }
		prefEditor.commit();
	}
	
   @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.id.menu_cancel, 0, R.string.menu_cancel).setIcon(R.drawable.ic_menu_close_clear_cancel);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_cancel: 
	        setResult(RESULT_CANCELED);
	        finish();
			return true;
		}
		return false;
	}
}
