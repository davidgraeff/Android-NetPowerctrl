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
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionHTTP;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.Utils;

/**
 * Create new device. This dialog is different to the device edit dialog. One HTTP/UDP connection data-set
 * can be entered (in contrast to the edit dialog, which hides connections by default and allow to have more
 * than one connection)
 */
public class DevicesWizardNewDialog extends DialogFragment implements onCreateDeviceResult {
    private EditDeviceInterface editDevice = null;
    private ArrayAdapter<String> ip_autocomplete;

    public DevicesWizardNewDialog() {
    }

    public void setPlugin(PluginInterface pluginInterface) {
        editDevice = pluginInterface.openEditDevice(null);
        editDevice.setResultListener(this);
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (editDevice == null)
            throw new RuntimeException("DevicesWizardNewFragment: editDevice not set");
        if (editDevice.getDevice() == null)
            throw new RuntimeException("DevicesWizardNewFragment: editDevice.getDevice not set");

        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_new, null);

        ip_autocomplete = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        EditText textView;

        List<Device> devices = AppData.getInstance().deviceCollection.getItems();
        for (Device device : devices)
            for (DeviceConnection deviceConnection : device.DeviceConnections) {
                ip_autocomplete.remove(deviceConnection.mHostName);
                ip_autocomplete.add(deviceConnection.mHostName);
            }
        AutoCompleteTextView iptext = (AutoCompleteTextView) view.findViewById(R.id.device_host);
        iptext.setAdapter(ip_autocomplete);

        textView = (EditText) view.findViewById(R.id.device_http_port);
        textView.setText("80");

        textView = (EditText) view.findViewById(R.id.device_udp_send);
        textView.setText(String.valueOf(SharedPrefs.getInstance().getDefaultSendPort()));

        textView = (EditText) view.findViewById(R.id.device_udp_receive);
        textView.setText(String.valueOf(SharedPrefs.getInstance().getDefaultReceivePort()));

        Device device = editDevice.getDevice();

        textView = (EditText) view.findViewById(R.id.device_username);
        textView.setText(device.UserName);

        textView = (EditText) view.findViewById(R.id.device_password);
        textView.setText(device.Password);

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
        builder.setTitle(R.string.device_add)
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
        editDevice.setResultListener(this);

        TextView textView;

        textView = (TextView) getDialog().findViewById(R.id.device_host);
        String hostname = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_http_port);
        int httpPort = -1;
        try {
            httpPort = Integer.valueOf(textView.getText().toString());
        } catch (NumberFormatException ignored) {
        }

        if (hostname.trim().isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }
        textView = (TextView) getDialog().findViewById(R.id.device_udp_receive);
        int udpReceive = -1;
        try {
            udpReceive = Integer.valueOf(textView.getText().toString());
        } catch (NumberFormatException ignored) {
        }

        textView = (TextView) getDialog().findViewById(R.id.device_udp_send);
        int udpSend = -1;
        try {
            udpSend = Integer.valueOf(textView.getText().toString());
        } catch (NumberFormatException ignored) {
        }

        if (hostname.trim().isEmpty() || udpReceive == -1 || udpSend == -1 || httpPort == -1) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Utils.checkPortInvalid(udpReceive)) {
            Utils.askForRootPorts(getActivity());
            return;
        }

        Device device = editDevice.getDevice();

        device.makeTemporaryDevice();
        device.DeviceConnections.clear();

        DeviceConnection deviceConnection = new DeviceConnectionHTTP(device, hostname, httpPort);
        deviceConnection.setIsAssignedByDevice(true);
        device.addConnection(deviceConnection);

        deviceConnection = new DeviceConnectionUDP(device, hostname, udpReceive, udpSend);
        deviceConnection.setIsAssignedByDevice(true);
        device.addConnection(deviceConnection);

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        device.UserName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        device.Password = textView.getText().toString();

        if (ip_autocomplete.getPosition(device.DeviceConnections.get(0).mHostName) != -1) {
            Toast.makeText(getActivity(), R.string.device_already_exist, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!editDevice.startTest(getActivity())) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void testFinished(boolean success) {
        if (success) {
            final Device deviceToAdd = editDevice.getDevice();
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    AppData.getInstance().addToConfiguredDevices(getActivity(),
                            deviceToAdd);
                }
            });
            dismiss();
        } else {
            Toast.makeText(getActivity(), R.string.device_test_not_reachable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void testDeviceNotReachable() {
        editDevice = null;
        Toast.makeText(getActivity(), R.string.error_device_no_access, Toast.LENGTH_SHORT).show();
    }
}