package oly.netpowerctrl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class DevicePreferences extends PreferenceActivity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String prefname = null;
		Intent it = getIntent();
		if (it != null) {
			Bundle extra = it.getExtras();
			if (extra != null) {
				Object o = extra.get("prefname");
				if (o != null) {
					prefname = (String) o; 
				}
			}
		}
		if (prefname == null) {
			Toast.makeText(this,
					getResources().getString(R.string.error_unknown_device),
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
        
        getPreferenceManager().setSharedPreferencesName(SharedPrefs.getFullPrefname(prefname));
		
        addPreferencesFromResource(R.xml.device_preferences);
        setTitle(getPreferenceManager().getSharedPreferences().getString(SharedPrefs.PREF_NAME, getResources().getText(R.string.default_device_name).toString()));
		
		findPreference(SharedPrefs.PREF_NAME).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setTitle(newValue.toString());
				return true;
			}
		});
		
		final Activity self = this;

		findPreference(SharedPrefs.PREF_SENDPORT+"_str").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int port = -1;
				try {
					port = Integer.parseInt(newValue.toString());
				} catch(NumberFormatException nfe) {
					Toast.makeText(self,
							getResources().getString(R.string.error_port_config_number),
							Toast.LENGTH_LONG).show();
					return false;
				} 
				
		        getPreferenceManager().getSharedPreferences()
		          .edit().putInt(SharedPrefs.PREF_SENDPORT, port);
				return true;
			}
		});
		
		findPreference(SharedPrefs.PREF_RECVPORT+"_str").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int port = -1;
				try {
					port = Integer.parseInt(newValue.toString());
				} catch(NumberFormatException nfe) {
					Toast.makeText(self,
							getResources().getString(R.string.error_port_config_number),
							Toast.LENGTH_LONG).show();
					return false;
				} 
				
		        getPreferenceManager().getSharedPreferences()
		          .edit().putInt(SharedPrefs.PREF_RECVPORT, port);
		        
		        DeviceQuery.restartDiscovery(self);  // port may have changed
				return true;
			}
		});
		
		final String f_prefname = prefname;
		findPreference(this.getResources().getText(R.string.setting_outlets)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent it = new Intent(self, OutletConfig.class);
				it.putExtra("prefname", f_prefname);
				startActivity(it);
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
