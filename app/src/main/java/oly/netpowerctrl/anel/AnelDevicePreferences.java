package oly.netpowerctrl.anel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;
import oly.netpowerctrl.devices.DeviceConnectionPreferences;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.DoneCancelFragmentHelper;

public class AnelDevicePreferences extends PreferenceFragment implements DeviceObserverResult, DeviceUpdate {
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    private TestStates test_state = TestStates.TEST_INIT;
    private Device device;
    private DeviceQuery deviceQuery;

    public AnelDevicePreferences() {
    }

    @Override
    public void onStart() {
        super.onStart();
        doneCancelFragmentHelper.addCancelDone(getActivity(), R.layout.device_done);
        doneCancelFragmentHelper.setTitle(getActivity(), R.string.configure_device);

        Activity a = getActivity();
        View btnDone = a.findViewById(R.id.action_mode_save_button);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveDevice();
            }
        });
        View btnTest = a.findViewById(R.id.action_mode_test_button);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testDevice();
            }
        });
        View btnCancel = a.findViewById(R.id.action_mode_close_button);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                getFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onStop() {
        doneCancelFragmentHelper.restoreTitle(getActivity());
        doneCancelFragmentHelper.restoreActionBar(getActivity());
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_preferences);

        /** Getting the ListPreference from the Preference Resource */
        final PreferenceManager m = getPreferenceManager();
        Preference p;
        p = m.findPreference("anel_name");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.DeviceName = (String) o;
                preference.setTitle(getString(R.string.device_name) + ": " + device.DeviceName);
                return true;
            }
        });
        ((EditTextPreference) p).setText(device.DeviceName);
        p.setTitle(getString(R.string.device_name) + ": " + device.DeviceName);

        p = m.findPreference("anel_username");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.UserName = (String) o;
                preference.setTitle(getString(R.string.device_username) + ": " + device.UserName);
                return true;
            }
        });
        ((EditTextPreference) p).setText(device.UserName);
        p.setTitle(getString(R.string.device_username) + ": " + device.UserName);

        p = m.findPreference("anel_password");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.Password = (String) o;
                preference.setTitle(getString(R.string.device_password) + ": " + device.Password);
                return true;
            }
        });
        ((EditTextPreference) p).setText(device.Password);
        p.setTitle(getString(R.string.device_password) + ": " + device.Password);

        p = m.findPreference("anel_enabled");
        ((CheckBoxPreference) p).setChecked(device.isEnabled());
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.setEnabled((Boolean) o);
                return true;
            }
        });

        p = m.findPreference("connections_http_new");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String host = "";

                if (device.DeviceConnections.size() > 0)
                    host = device.DeviceConnections.get(0).getDestinationHost();
                DeviceConnection deviceConnection = new DeviceConnectionHTTP(device, host, 80);
                DeviceConnectionPreferences fragment = new DeviceConnectionPreferences();
                fragment.setDeviceConnection(deviceConnection, device, true);

                //noinspection ConstantConditions
                getFragmentManager().beginTransaction().addToBackStack(null).
                        setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                        replace(R.id.content_frame, fragment).commit();
                return true;
            }
        });
        p = m.findPreference("connections_udp_new");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String host = "";
                if (device.DeviceConnections.size() > 0)
                    host = device.DeviceConnections.get(0).getDestinationHost();
                DeviceConnection deviceConnection = new DeviceConnectionUDP(device, host,
                        SharedPrefs.getDefaultReceivePort(), SharedPrefs.getDefaultSendPort());
                DeviceConnectionPreferences fragment = new DeviceConnectionPreferences();
                fragment.setDeviceConnection(deviceConnection, device, true);

                //noinspection ConstantConditions
                getFragmentManager().beginTransaction().addToBackStack(null).
                        setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                        replace(R.id.content_frame, fragment).commit();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        update_connections();
        super.onResume();
    }

    private void update_connections() {
        PreferenceCategory pc_http = (PreferenceCategory) findPreference("connections_http");
        PreferenceCategory pc_udp = (PreferenceCategory) findPreference("connections_udp");
        assert pc_http != null;
        assert pc_udp != null;

        for (int i = pc_http.getPreferenceCount() - 1; i >= 0; --i) {
            Preference s = pc_http.getPreference(i);
            if (!s.getKey().equals("connections_http_new")) {
                pc_http.removePreference(s);
            }
        }
        for (int i = pc_udp.getPreferenceCount() - 1; i >= 0; --i) {
            Preference s = pc_udp.getPreference(i);
            if (!s.getKey().equals("connections_udp_new")) {
                pc_udp.removePreference(s);
            }
        }


        int id = 0;
        for (final DeviceConnection deviceConnection : device.DeviceConnections) {
            ++id;
            //noinspection ConstantConditions
            PreferenceScreen s = getPreferenceManager().createPreferenceScreen(getActivity());
            assert s != null;
            s.setKey(String.valueOf(id));
            //s.setFragment(WidgetPreferenceFragment.class.getName());
            s.setTitle(deviceConnection.getString());
            if (deviceConnection instanceof DeviceConnectionUDP) {
                pc_udp.addPreference(s);
            } else if (deviceConnection instanceof DeviceConnectionHTTP) {
                pc_http.addPreference(s);
            }

            s.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
//                    Fragment fragment = Fragment.instantiate(getActivity(),
//                            DeviceConnectionPreferences.class.getName());
//                    ((DeviceConnectionPreferences) fragment).setDeviceConnection(deviceConnection);

                    DeviceConnectionPreferences fragment = new DeviceConnectionPreferences();
                    fragment.setDeviceConnection(deviceConnection, device, false);

                    //noinspection ConstantConditions
                    getFragmentManager().beginTransaction().addToBackStack(null).
                            setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                            replace(R.id.content_frame, fragment).commit();
                    return true;
                }
            });
        }
    }

    public void setDevice(Device di) {
        device = di;
        if (device == null) {
            device = Device.createNewDevice(AnelUDPDeviceDiscoveryThread.anelPlugin.getPluginID());
            // Default values for user and password
            device.UserName = "admin";
            device.Password = "anel";
        }
    }

    private void testDevice() {
        if (test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK)
            return;

        if (device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        test_state = TestStates.TEST_REACHABLE;

        PluginInterface pluginInterface = device.getPluginInterface();
        if (pluginInterface == null) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }
        pluginInterface.enterFullNetworkState(device);

        deviceQuery = new DeviceQuery(this, device);
    }

    private void saveDevice() {
        if (device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (test_state != TestStates.TEST_OK) {
            //noinspection ConstantConditions
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
            return;
        }

        saveAndFinish();
    }

    private void saveAndFinish() {
        PluginInterface pluginInterface = device.getPluginInterface();
        if (pluginInterface != null) {
            pluginInterface.enterFullNetworkState(device);
        }

        NetpowerctrlApplication.getDataController().addToConfiguredDevices(device);
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDeviceUpdated(Device di) {
        onDeviceUpdated(di, false);
    }

    @Override
    public void onDeviceUpdated(Device updated_device, boolean willBeRemoved) {
        if (!updated_device.equalsByUniqueID(device))
            return;

        if (test_state == TestStates.TEST_REACHABLE) {
            // Update stored device with received values
            device.UniqueDeviceID = updated_device.UniqueDeviceID;
            // do not copy the deviceName here, just the other values
            device.copyValuesFromUpdated(updated_device);
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            PluginInterface pi = device.getPluginInterface();
            assert pi != null;
            if (deviceQuery != null) {
                deviceQuery.addDevice(device, false);
            }
            DevicePort oi = device.getFirst();
            if (oi != null)
                pi.execute(oi, oi.current_value, null);
            Handler handler = new Handler();
            // Timeout is 1,1s
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (test_state == TestStates.TEST_ACCESS || test_state == TestStates.TEST_INIT) {
                        test_state = TestStates.TEST_INIT;
                        //noinspection ConstantConditions
                        Toast.makeText(getActivity(),
                                getActivity().getString(R.string.error_device_no_access) + ": " + device.UserName + " " + device.Password,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }, 1100);
        } else if (test_state == TestStates.TEST_ACCESS) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getActivity().getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();
            device.copyValuesFromUpdated(updated_device);
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        if (test_state != TestStates.TEST_REACHABLE)
            return;

        for (Device di : timeout_devices) {
            if (!di.equalsByUniqueID(device))
                continue;
            test_state = TestStates.TEST_INIT;
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getActivity().getString(R.string.error_device_not_reachable) + ": " + device.DeviceName + ":"
                            + Integer.valueOf(0).toString(),
                    Toast.LENGTH_SHORT
            ).show();
            break;
        }
    }

//    @Override
//    public void onDeviceError(DeviceInfo di) {
//        if (test_state == TestStates.TEST_ACCESS) {
//            if (di.equalsByUniqueID(device)) {
//                test_state = TestStates.TEST_INIT;
//            }
//        }
//    }

    private enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}
}