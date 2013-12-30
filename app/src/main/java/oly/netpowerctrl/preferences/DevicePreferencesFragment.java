package oly.netpowerctrl.preferences;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.service.DeviceQuery;

/**
 */
public class DevicePreferencesFragment extends PreferenceFragment {
    private static final String ARG_PARAM1 = "prefname";
    private String prefname = null;

    private DevicePreferencesFragment() {
    }

    public static DevicePreferencesFragment instantiate(Context ctx) {
        DevicePreferencesFragment fragment = (DevicePreferencesFragment) Fragment.instantiate(ctx, DevicePreferencesFragment.class.getName());
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, SharedPrefs.PREF_TEMPDEVICE);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.device_preferences, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_test_device: {
                DeviceInfo di = SharedPrefs.ReadTempDevice(getActivity());
                DeviceQuery.sendQuery(getActivity(), di.HostName, di.SendPort);
                return true;
            }

            case R.id.menu_save_device: {
                NetpowerctrlApplication.instance.addToConfiguredDevices(SharedPrefs.ReadTempDevice(getActivity()));
                //noinspection ConstantConditions
                getFragmentManager().popBackStack();
                return true;
            }

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            prefname = getArguments().getString(ARG_PARAM1);
        }

        if (prefname == null) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getResources().getString(R.string.error_unknown_device),
                    Toast.LENGTH_LONG).show();
            //noinspection ConstantConditions
            getFragmentManager().popBackStack();
            return;
        }


        //noinspection ConstantConditions
        getPreferenceManager().setSharedPreferencesName(prefname);


        addPreferencesFromResource(R.xml.device_preferences);
        //setTitle(getPreferenceManager().getSharedPreferences().getString(SharedPrefs.PREF_NAME, getResources().getText(R.string.default_device_name).toString()));

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_NAME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //setTitle(newValue.toString());
                return true;
            }
        });

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_SENDPORT + "_str").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int port;
                try {
                    port = Integer.parseInt(newValue.toString());
                } catch (NumberFormatException nfe) {
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.error_port_config_number),
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                //noinspection ConstantConditions
                getPreferenceManager().getSharedPreferences()
                        .edit().putInt(SharedPrefs.PREF_SENDPORT, port);
                return true;
            }
        });

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_RECVPORT + "_str").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int port;
                try {
                    port = Integer.parseInt(newValue.toString());
                } catch (NumberFormatException nfe) {
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.error_port_config_number),
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                //noinspection ConstantConditions
                getPreferenceManager().getSharedPreferences()
                        .edit().putInt(SharedPrefs.PREF_RECVPORT, port);

                NetpowerctrlApplication.instance.restartListening();  // port may have changed
                return true;
            }
        });
    }
}
