package oly.netpowerctrl.devices;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;

import oly.netpowerctrl.R;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.DoneCancelFragmentHelper;

public class DeviceConnectionPreferences extends PreferenceFragment {
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    private DeviceConnectionUDP deviceConnection_udp;
    private DeviceConnectionHTTP deviceConnection_http;
    private boolean newConnection;
    private Device device;

    public DeviceConnectionPreferences() {
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
        doneCancelFragmentHelper.addCancelDone(getActivity(), R.layout.device_connection_bar);
        if (deviceConnection_http != null) {
            doneCancelFragmentHelper.setTitle(getActivity(), deviceConnection_http.getString());
        } else if (deviceConnection_udp != null) {
            doneCancelFragmentHelper.setTitle(getActivity(), deviceConnection_udp.getString());
        }

        Activity a = getActivity();
        View btnTest = a.findViewById(R.id.action_mode_remove_button);
//        if (newConnection)
//            btnTest.setVisibility(View.GONE);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (device != null && !newConnection) {
                    if (deviceConnection_http != null) {
                        device.removeConnection(deviceConnection_http);
                    } else if (deviceConnection_udp != null) {
                        device.removeConnection(deviceConnection_udp);
                    }
                }
                device = null;
                //noinspection ConstantConditions
                getFragmentManager().popBackStack();
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
    public void onPause() {
        if (device != null && newConnection) {
            if (deviceConnection_http != null) {
                device.addConnection(deviceConnection_http);
            } else if (deviceConnection_udp != null) {
                device.addConnection(deviceConnection_udp);
            }
        }
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (deviceConnection_http != null) {
            addPreferencesFromResource(R.xml.device_preferences_http);
        } else if (deviceConnection_udp != null) {
            addPreferencesFromResource(R.xml.device_preferences_udp);
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
                        Utils.warn_port(getActivity());
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