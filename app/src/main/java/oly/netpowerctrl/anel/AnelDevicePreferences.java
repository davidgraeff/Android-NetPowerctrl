package oly.netpowerctrl.anel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.DoneCancelFragmentHelper;

public class AnelDevicePreferences extends PreferenceFragment implements DeviceObserverResult, DeviceUpdate {
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    private TestStates test_state = TestStates.TEST_INIT;
    private DeviceInfo device;
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
        addPreferencesFromResource(R.xml.anel_device_preferences);

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

        p = m.findPreference("anel_ip");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.HostName = (String) o;
                preference.setTitle(getString(R.string.device_ip) + ": " + device.HostName);
                return true;
            }
        });
        ((EditTextPreference) p).setText(device.HostName);
        p.setTitle(getString(R.string.device_ip) + ": " + device.HostName);

        p = m.findPreference("anel_use_http");
        ((CheckBoxPreference) p).setChecked(device.PreferHTTP);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.PreferHTTP = (Boolean) o;
                checkEnabled();
                return true;
            }
        });

        p = m.findPreference("anel_enabled");
        ((CheckBoxPreference) p).setChecked(device.enabled);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.enabled = (Boolean) o;
                checkEnabled();
                return true;
            }
        });

        p = m.findPreference("anel_use_default_udp");
        ((CheckBoxPreference) p).setChecked(device.DefaultPorts);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                device.DefaultPorts = (Boolean) o;
                checkEnabled();
                return true;
            }
        });

        p = m.findPreference("anel_udp_send");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int port = Integer.valueOf((String) o);
                if (Utils.checkPortInvalid(port)) {
                    Utils.warn_port(getActivity());
                    return false;
                }
                preference.setTitle(getString(R.string.device_send_udp_port) + ": " + String.valueOf(port));
                device.SendPort = port;
                return true;
            }
        });
        ((EditTextPreference) p).setText(String.valueOf(device.SendPort));
        p.setTitle(getString(R.string.device_send_udp_port) + ": " + String.valueOf(device.SendPort));

        p = m.findPreference("anel_udp_receive");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int port = Integer.valueOf((String) o);
                if (Utils.checkPortInvalid(port)) {
                    Utils.warn_port(getActivity());
                    return false;
                }
                preference.setTitle(getString(R.string.device_receive_udp_port) + ": " + String.valueOf(port));
                device.ReceivePort = port;
                return true;
            }
        });
        ((EditTextPreference) p).setText(String.valueOf(device.ReceivePort));
        p.setTitle(getString(R.string.device_receive_udp_port) + ": " + String.valueOf(device.ReceivePort));

        p = m.findPreference("anel_http");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int port = Integer.valueOf((String) o);
                if (port != 80 && Utils.checkPortInvalid(port)) {
                    Utils.warn_port(getActivity());
                    return false;
                }
                preference.setTitle(getString(R.string.device_http_port) + ": " + String.valueOf(port));
                device.HttpPort = port;
                return true;
            }
        });
        ((EditTextPreference) p).setText(String.valueOf(device.HttpPort));
        p.setTitle(getString(R.string.device_http_port) + ": " + String.valueOf(device.HttpPort));

        checkEnabled();
    }

    private void checkEnabled() {
        PreferenceManager m = getPreferenceManager();
        m.findPreference("anel_use_default_udp").setEnabled(!device.PreferHTTP);
        m.findPreference("anel_udp_send").setEnabled(!device.PreferHTTP && !device.DefaultPorts);
        m.findPreference("anel_udp_receive").setEnabled(!device.PreferHTTP && !device.DefaultPorts);
        m.findPreference("anel_http").setEnabled(device.PreferHTTP);
    }

    public void setDevice(DeviceInfo di) {
        device = di;
        if (device == null) {
            device = DeviceInfo.createNewDevice(AnelDeviceDiscoveryThread.anelPlugin.getPluginID());
            // Default values for user and password
            device.UserName = "admin";
            device.Password = "anel";
        }
    }

    private void testDevice() {
        if (test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK)
            return;

        if (device.HostName.isEmpty() || device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        test_state = TestStates.TEST_REACHABLE;

        PluginInterface pi = device.getPluginInterface(NetpowerctrlService.getService());
        assert pi != null;
        pi.prepareForDevices(device);

        deviceQuery = new DeviceQuery(this, device);
    }

    private void saveDevice() {
        if (device.HostName.isEmpty() || device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
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
        PluginInterface pi = device.getPluginInterface(NetpowerctrlService.getService());
        assert pi != null;
        pi.prepareForDevices(device);

        NetpowerctrlApplication.getDataController().addToConfiguredDevices(device);
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        onDeviceUpdated(di, false);
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        if (!di.HostName.equals(device.HostName))
            return;

        if (test_state == TestStates.TEST_REACHABLE) {
            // Update stored device with received values
            device.UniqueDeviceID = di.UniqueDeviceID;
            // do not copy the deviceName here, just the other values
            device.copyFreshValues(di);
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            PluginInterface pi = device.getPluginInterface(NetpowerctrlService.getService());
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
            device.setReachable();
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
        for (DeviceInfo di : timeout_devices) {
            if (test_state == TestStates.TEST_REACHABLE) {
                test_state = TestStates.TEST_INIT;
                //noinspection ConstantConditions
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.error_device_not_reachable) + ": " + device.HostName + ":"
                                + Integer.valueOf((device.PreferHTTP ? device.HttpPort : device.SendPort)).toString(),
                        Toast.LENGTH_SHORT
                ).show();
            }
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