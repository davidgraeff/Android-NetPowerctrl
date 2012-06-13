package oly.netpowerctrl;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;


public class DevicePreferences extends PreferenceActivity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean new_device = getIntent().getExtras().getBoolean("new_device");
        Intent ret_intent = new Intent(); // for returning data to onActivityResult()
        ret_intent.putExtra("SharedPreferencesName", getPreferenceManager().getSharedPreferencesName());
        setResult(RESULT_OK, ret_intent); // default
        
        SharedPreferences pref = getSharedPreferences(getPreferenceManager().getSharedPreferencesName(), MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();

        int default_send_port = getResources().getInteger(R.integer.default_send_port);
        int default_recv_port = getResources().getInteger(R.integer.default_send_port);
        
        if (new_device) {
        	setTitle(getResources().getText(R.string.default_device_name));
        	prefEditor.putString("setting_device_name", "");
			prefEditor.putString("setting_device_ip", "");
			prefEditor.putBoolean("setting_standard_ports", true);
			prefEditor.putString("setting_send_udp", String.format("%d",default_send_port));
			prefEditor.putString("setting_recv_udp", String.format("%d",default_recv_port));
			prefEditor.putString("setting_username", "");
			prefEditor.putString("setting_password", "");
        } else {
            DeviceInfo device_info = (DeviceInfo) getIntent().getExtras().get("device_info");
            ret_intent.putExtra("uuid", device_info.uuid);
        	setTitle(device_info.DeviceName);
        	prefEditor.putString("setting_device_name", device_info.DeviceName);
			prefEditor.putString("setting_device_ip", device_info.HostName);
			boolean default_ports = (device_info.SendPort == default_send_port) && (device_info.RecvPort == default_recv_port); 
			prefEditor.putBoolean("setting_standard_ports", default_ports);
			prefEditor.putString("setting_send_udp", String.format("%d",device_info.SendPort));
			prefEditor.putString("setting_recv_udp", String.format("%d",device_info.RecvPort));
			prefEditor.putString("setting_username", device_info.UserName);
			prefEditor.putString("setting_password", device_info.Password);
        }
		prefEditor.commit();
		
        addPreferencesFromResource(R.xml.device_preferences);
		
		Preference p = findPreference("setting_device_name");
		p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setTitle(newValue.toString());
				return true;
			}
		});
    }
	
   @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.id.menu_cancel, 0, R.string.menu_cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
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
