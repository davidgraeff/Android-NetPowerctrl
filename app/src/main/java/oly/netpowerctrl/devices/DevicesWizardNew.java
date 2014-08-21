package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelCreateDevice;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 20.08.14.
 */
public class DevicesWizardNew extends DialogFragment implements AnelCreateDevice.AnelCreateDeviceResult {
    AnelCreateDevice anelCreateDevice = new AnelCreateDevice(null);
    ArrayAdapter<String> ip_autocomple;

    public DevicesWizardNew() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_new, null);

        ip_autocomple = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        EditText textView;

        List<Device> devices = NetpowerctrlApplication.getDataController().deviceCollection.devices;
        for (Device device : devices)
            for (DeviceConnection deviceConnection : device.DeviceConnections) {
                ip_autocomple.remove(deviceConnection.HostName);
                ip_autocomple.add(deviceConnection.HostName);
            }
        AutoCompleteTextView iptext = (AutoCompleteTextView) view.findViewById(R.id.device_ip);
        iptext.setAdapter(ip_autocomple);

        textView = (EditText) view.findViewById(R.id.device_http_port);
        textView.setText("80");

        textView = (EditText) view.findViewById(R.id.device_udp_send);
        textView.setText(String.valueOf(SharedPrefs.getDefaultSendPort()));

        textView = (EditText) view.findViewById(R.id.device_udp_receive);
        textView.setText(String.valueOf(SharedPrefs.getDefaultReceivePort()));

        textView = (EditText) view.findViewById(R.id.device_username);
        textView.setText(anelCreateDevice.device.UserName);

        textView = (EditText) view.findViewById(R.id.device_password);
        textView.setText(anelCreateDevice.device.Password);

        view.findViewById(R.id.device_ip_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_ip_summary, Toast.LENGTH_LONG).show();
            }
        });
        view.findViewById(R.id.device_username_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_username_summary, Toast.LENGTH_LONG).show();
            }
        });
        view.findViewById(R.id.device_password_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_password_summary, Toast.LENGTH_LONG).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_add_device)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        Button button = d.getButton(Dialog.BUTTON_POSITIVE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });
    }

    void startTest() {
        anelCreateDevice.listener = this;

        TextView textView;

        textView = (TextView) getDialog().findViewById(R.id.device_ip);
        String hostname = textView.getText().toString();

        if (hostname.trim().isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        textView = (TextView) getDialog().findViewById(R.id.device_http_port);
        int httpPort = Integer.valueOf(textView.getText().toString());

        textView = (TextView) getDialog().findViewById(R.id.device_udp_receive);
        int updReceive = Integer.valueOf(textView.getText().toString());

        textView = (TextView) getDialog().findViewById(R.id.device_udp_send);
        int udpSend = Integer.valueOf(textView.getText().toString());

        if (udpSend < 1024 && !SharedPrefs.isPortsUnlimited()) {
            Toast.makeText(getActivity(), R.string.port_warning_1024, Toast.LENGTH_SHORT).show();
            return;
        }

        anelCreateDevice.device.UniqueDeviceID = null;
        anelCreateDevice.device.DeviceConnections.clear();
        anelCreateDevice.device.addConnection(new DeviceConnectionHTTP(anelCreateDevice.device, hostname, httpPort));
        anelCreateDevice.device.addConnection(new DeviceConnectionUDP(anelCreateDevice.device, hostname, updReceive, udpSend));

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        anelCreateDevice.device.UserName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        anelCreateDevice.device.Password = textView.getText().toString();

        if (ip_autocomple.getPosition(anelCreateDevice.device.DeviceConnections.get(0).HostName) != -1) {
            Toast.makeText(getActivity(), R.string.device_already_exist, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!anelCreateDevice.startTest()) {
            anelCreateDevice.listener = null;
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void testFinished(boolean success) {
        if (success) {
            final Device deviceToAdd = anelCreateDevice.device;
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    NetpowerctrlApplication.getDataController().addToConfiguredDevices(deviceToAdd);
                }
            });
            dismiss();
        } else {
            Toast.makeText(getActivity(), R.string.error_device_not_reachable, Toast.LENGTH_SHORT).show();
        }
        anelCreateDevice.listener = null;
    }

    @Override
    public void testDeviceNotReachable() {
        anelCreateDevice.listener = null;
        anelCreateDevice = null;
        Toast.makeText(getActivity(), R.string.error_device_no_access, Toast.LENGTH_SHORT).show();
    }
}