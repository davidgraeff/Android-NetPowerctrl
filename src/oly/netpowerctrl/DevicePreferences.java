package oly.netpowerctrl;

import java.util.UUID;

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

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean new_device = true;
        String prefname = null;
		Intent it = getIntent();
		if (it != null) {
			Bundle extra = it.getExtras();
			if (extra != null) {
				Object o = extra.get("new_device");
				if (o != null) {
					new_device = (Boolean) o; 
				}

				o = extra.get("prefname");
				if (o != null) {
					prefname = (String) o; 
				}
			}
		}
		if (prefname == null)
				new_device = true; // fallback
		
        if (new_device) { 
			setTitle(R.string.default_device_name);
			prefname = DeviceInfo.makePrefname(UUID.randomUUID());
        }
        
        getPreferenceManager().setSharedPreferencesName(SharedPrefs.PREF_BASENAME+"."+prefname);
        //TODO setTitle(device_info.DeviceName);
		
        addPreferencesFromResource(R.xml.device_preferences);
		
		Preference p = findPreference(SharedPrefs.PREF_NAME);
		p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setTitle(newValue.toString());
				return true;
			}
		});
		
		Preference myPref = (Preference) findPreference(this.getResources().getText(R.string.setting_outlets));
		final Activity self = this;
		final String f_prefname = prefname;
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent it = new Intent(self, OutletConfig.class);
				it.putExtra("device_info", f_prefname);
				startActivityForResult(it, R.id.request_code_config_outlets);
			    return true;
			}
		});

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
