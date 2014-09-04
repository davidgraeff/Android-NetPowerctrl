package oly.netpowerctrl.devices;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;

import oly.netpowerctrl.R;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.actionbar.ActionBarDoneCancel;

//TODO rework
public class DeviceConnectionPreferences extends PreferenceFragment {
    ActionBarDoneCancel actionBarDoneCancel = new ActionBarDoneCancel();
    private DeviceConnectionUDP deviceConnection_udp;
    private DeviceConnectionHTTP deviceConnection_http;
    private boolean newConnection;
    private Device device;

    public DeviceConnectionPreferences() {
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
            if (!deviceConnection.isCustom()) {
                s.setEnabled(false);
                continue;
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
                    // replace(R.id.content_frame, fragment).commit();
                    return true;
                }
            });
        }
    }

    public void setDeviceConnection(DeviceConnection c, Device device, boolean newConnection) {
        this.newConnection = newConnection;
        this.device = device;
        if (c instanceof DeviceConnectionHTTP)
            deviceConnection_http = (DeviceConnectionHTTP) c;
        else if (c instanceof DeviceConnectionUDP)
            deviceConnection_udp = (DeviceConnectionUDP) c;
    }


    @Override
    public void onStart() {
        super.onStart();
        actionBarDoneCancel.addCancelDone(getActivity(), R.layout.device_connection_bar);
        if (deviceConnection_http != null) {
            actionBarDoneCancel.setTitle(getActivity(), deviceConnection_http.getString());
        } else if (deviceConnection_udp != null) {
            actionBarDoneCancel.setTitle(getActivity(), deviceConnection_udp.getString());
        }

        Activity a = getActivity();
        View btnTest = a.findViewById(R.id.action_mode_remove_button);
//        if (newConnection)
//            btnTest.setVisibility(View.GONE);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // Delete connection
                if (device != null && !newConnection) {
                    if (deviceConnection_http != null) {
                        device.removeConnection(deviceConnection_http);
                    } else if (deviceConnection_udp != null) {
                        device.removeConnection(deviceConnection_udp);
                    }
                }
                //noinspection ConstantConditions
            }
        });
        View btnCancel = a.findViewById(R.id.action_mode_close_button);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
            }
        });
    }

    @Override
    public void onStop() {
        device = null;

        actionBarDoneCancel.restoreTitle(getActivity());
        actionBarDoneCancel.restoreActionBar(getActivity());
        super.onStop();
    }

    @Override
    public void onPause() {
        if (device != null && newConnection) {
            if (deviceConnection_http != null &&
                    deviceConnection_http.getDestinationHost().length() > 0 &&
                    deviceConnection_http.getListenPort() > 0) {
                device.addConnection(deviceConnection_http);
            } else if (deviceConnection_udp != null &&
                    deviceConnection_udp.getDestinationHost().length() > 0 &&
                    deviceConnection_udp.getListenPort() > 0) {
                device.addConnection(deviceConnection_udp);
            }
        }
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (deviceConnection_http != null) {
//            addPreferencesFromResource(R.xml.device_preferences_http);
        } else if (deviceConnection_udp != null) {
//            addPreferencesFromResource(R.xml.device_preferences_udp);
        } else {
            return;
        }

        /** Getting the ListPreference from the Preference Resource */
        final PreferenceManager m = getPreferenceManager();
        Preference p;

        p = m.findPreference("ip");
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (deviceConnection_http != null) {
                    deviceConnection_http.HostName = (String) o;
                    preference.setTitle(getString(R.string.device_ip) + ": " +
                            deviceConnection_http.HostName);
                } else if (deviceConnection_udp != null) {
                    deviceConnection_udp.HostName = (String) o;
                    preference.setTitle(getString(R.string.device_ip) + ": " +
                            deviceConnection_udp.HostName);
                }
                return true;
            }
        });
        if (deviceConnection_http != null) {
            ((EditTextPreference) p).setText(deviceConnection_http.getDestinationHost());
            p.setTitle(getString(R.string.device_ip) + ": " + deviceConnection_http.getDestinationHost());
        } else if (deviceConnection_udp != null) {
            ((EditTextPreference) p).setText(deviceConnection_udp.getDestinationHost());
            p.setTitle(getString(R.string.device_ip) + ": " + deviceConnection_udp.getDestinationHost());
        }


        p = m.findPreference("use_default_ports");
        if (deviceConnection_http != null)
            ((CheckBoxPreference) p).setChecked(deviceConnection_http.DefaultPorts);
        else if (deviceConnection_udp != null)
            ((CheckBoxPreference) p).setChecked(deviceConnection_udp.DefaultPorts);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (deviceConnection_http != null)
                    deviceConnection_http.DefaultPorts = (Boolean) o;
                else if (deviceConnection_udp != null)
                    deviceConnection_udp.DefaultPorts = (Boolean) o;
                return true;
            }
        });

        if (deviceConnection_udp != null) {
            p = m.findPreference("udp_send_port");
            p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    int port = Integer.valueOf((String) o);
                    preference.setTitle(getString(R.string.device_send_udp_port) + ": " + String.valueOf(port));
                    deviceConnection_udp.PortUDPSend = port;
                    return true;
                }
            });
            ((EditTextPreference) p).setText(String.valueOf(deviceConnection_udp.PortUDPSend));
            p.setTitle(getString(R.string.device_send_udp_port) + ": " + String.valueOf(deviceConnection_udp.PortUDPSend));

            p = m.findPreference("udp_receive_port");
            p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    int port = Integer.valueOf((String) o);
                    if (Utils.checkPortInvalid(port)) {
                        Utils.askForRootPorts(getActivity());
                        return false;
                    }
                    preference.setTitle(getString(R.string.device_receive_udp_port) + ": " + String.valueOf(port));
                    deviceConnection_udp.PortUDPReceive = port;
                    return true;
                }
            });
            ((EditTextPreference) p).setText(String.valueOf(deviceConnection_udp.PortUDPReceive));
            p.setTitle(getString(R.string.device_receive_udp_port) + ": " + String.valueOf(deviceConnection_udp.PortUDPReceive));
        } else if (deviceConnection_http != null) {
            p = m.findPreference("http_port");
            p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    int port = Integer.valueOf((String) o);
                    preference.setTitle(getString(R.string.device_http_port) + ": " + String.valueOf(port));
                    deviceConnection_http.PortHttp = port;
                    return true;
                }
            });
            ((EditTextPreference) p).setText(String.valueOf(deviceConnection_http.PortHttp));
            p.setTitle(getString(R.string.device_http_port) + ": " + String.valueOf(deviceConnection_http.PortHttp));
        }
    }
}