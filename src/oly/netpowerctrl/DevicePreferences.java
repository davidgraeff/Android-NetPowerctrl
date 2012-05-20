package oly.netpowerctrl;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class DevicePreferences extends PreferenceActivity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.device_preferences);
		
		setTitle("holla");
    }
}
