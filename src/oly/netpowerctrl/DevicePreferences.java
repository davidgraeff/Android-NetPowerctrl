package oly.netpowerctrl;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;


public class DevicePreferences extends PreferenceActivity {

	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_preferences);
        boolean new_device = getIntent().getExtras().getInt("new_device") != 0;
        
        setResult(RESULT_OK); // default
        
        if (new_device)
        	setTitle(getIntent().getExtras().getCharSequence("New Device"));
		
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
