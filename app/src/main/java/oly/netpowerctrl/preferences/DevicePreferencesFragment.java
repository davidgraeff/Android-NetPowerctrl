package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceError;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DeviceUpdated;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 */
public class DevicePreferencesFragment extends PreferenceFragment implements DeviceUpdated, DeviceError {
    private static final String ARG_PARAM1 = "prefname";
    private String prefname = null;

    private enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}

    ;
    private TestStates test_state = TestStates.TEST_INIT;
    private DeviceInfo testDevice;

    public DevicePreferencesFragment() {
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
                if (test_state != TestStates.TEST_INIT)
                    return true;

                test_state = TestStates.TEST_REACHABLE;
                testDevice = SharedPrefs.ReadTempDevice(getActivity());
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (test_state == TestStates.TEST_REACHABLE) {
                            test_state = TestStates.TEST_INIT;
                            Toast.makeText(getActivity(), "Test reachable failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
                DeviceQuery.sendQuery(getActivity(), testDevice.HostName, testDevice.SendPort);
                return true;
            }

            case R.id.menu_save_device: {
                if (test_state != TestStates.TEST_OK) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.device_test)
                            .setMessage(R.string.device_save_without_test)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    saveAndFinish();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }

                saveAndFinish();
                return true;
            }

        }
        return false;
    }

    private void saveAndFinish() {
        NetpowerctrlApplication.instance.addToConfiguredDevices(SharedPrefs.ReadTempDevice(getActivity()));
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        NetpowerctrlApplication.instance.getService().unregisterDeviceUpdateObserver(this);
        NetpowerctrlApplication.instance.getService().unregisterDeviceErrorObserver(this);
        super.onDestroy();
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

        NetpowerctrlApplication.instance.getService().registerDeviceUpdateObserver(this);
        NetpowerctrlApplication.instance.getService().registerDeviceErrorObserver(this);

        //noinspection ConstantConditions
        getPreferenceManager().setSharedPreferencesName(prefname);


        addPreferencesFromResource(R.xml.device_preferences);
        //setTitle(getPreferenceManager().getSharedPreferences().getString(SharedPrefs.PREF_NAME, getResources().getText(R.string.default_device_name).toString()));

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_NAME).setOnPreferenceChangeListener(prefChanged);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_IP).setOnPreferenceChangeListener(prefChanged);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_PASSWORD).setOnPreferenceChangeListener(prefChanged);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_USERNAME).setOnPreferenceChangeListener(prefChanged);

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

                test_state = TestStates.TEST_INIT;
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

                test_state = TestStates.TEST_INIT;
                NetpowerctrlApplication.instance.restartListening();  // port may have changed
                return true;
            }
        });
    }

    private Preference.OnPreferenceChangeListener prefChanged = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            test_state = TestStates.TEST_INIT;
            return true;
        }
    };

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        if (test_state == TestStates.TEST_REACHABLE) {
            if (di.HostName.equals(testDevice.HostName)) {
                testDevice.MacAddress = di.MacAddress;
                testDevice.DeviceName = di.DeviceName;
                testDevice.Outlets.clear();
                testDevice.Outlets.addAll(di.Outlets);
                test_state = TestStates.TEST_ACCESS;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (test_state == TestStates.TEST_ACCESS) {
                            test_state = TestStates.TEST_INIT;
                            Toast.makeText(getActivity(), "Test access failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
                DeviceSend.DeviceSwitch ds = new DeviceSend.DeviceSwitch(testDevice);
                DeviceSend.sendAllOutlets(getActivity(), ds);
            }
        } else if (test_state == TestStates.TEST_ACCESS) {
            Toast.makeText(getActivity(), "Test OK", Toast.LENGTH_SHORT).show();
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onDeviceError(String devicename, String errMessage) {
        if (test_state == TestStates.TEST_REACHABLE) {
            Log.w("onDeviceError", devicename + " " + testDevice.HostName);
            if (devicename == testDevice.DeviceName) {
                Toast.makeText(getActivity(), "Test access failed: " + errMessage, Toast.LENGTH_SHORT).show();
                test_state = TestStates.TEST_INIT;
            }
        }
    }
}
