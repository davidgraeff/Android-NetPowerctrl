package oly.netpowerctrl;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;


public class DevicePreferences extends PreferenceActivity {

	DeviceInfo device_info;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.device_preferences);

        boolean new_device = getIntent().getExtras().getInt("new_device") != 0;
        device_info = (DeviceInfo) getIntent().getExtras().get("device_info");
        Intent it = new Intent(); // for returning data to onActivityResult()
        it.putExtra("device_info", device_info);
        setResult(RESULT_OK, it); // default
        
        if (new_device) {
        	setTitle(getIntent().getExtras().getCharSequence("New Device"));
        	//EditTextPreference p = (EditTextPreference) findPreference("setting_device_name");
        	//if (p != null) p.setText("");
//			prefEditor.putString("setting_device_ip", "");
//			prefEditor.putBoolean("setting_standard_ports", true);
//			prefEditor.putInt("setting_send_udp", R.integer.default_send_port);
//			prefEditor.putInt("setting_recv_udp", R.integer.default_recv_port);
//			prefEditor.putString("setting_username", "");
//			prefEditor.putString("setting_password", "");
        } else {
        	setTitle(device_info.DeviceName);
//        	prefEditor.putString("setting_device_name", device_info.DeviceName);
//			prefEditor.putString("setting_device_ip", device_info.HostName);
//			boolean default_ports = (device_info.SendPort == R.integer.default_send_port) && (device_info.RecvPort == R.integer.default_recv_port); 
//			prefEditor.putBoolean("setting_standard_ports", default_ports);
//			prefEditor.putInt("setting_send_udp", device_info.SendPort);
//			prefEditor.putInt("setting_recv_udp", device_info.RecvPort);
//			prefEditor.putString("setting_username", device_info.UserName);
//			prefEditor.putString("setting_password", device_info.Password);
        }
//		prefEditor.commit();
		
		Preference p = findPreference("setting_device_name");
		p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setTitle(newValue.toString());
				return false;
			}
		});
    }
	
}
