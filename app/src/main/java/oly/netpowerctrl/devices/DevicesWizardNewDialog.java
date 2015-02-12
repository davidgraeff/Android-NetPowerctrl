package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionHTTP;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.pluginservice.AbstractBasePlugin;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

/**
 * Create new device. This dialog is different to the device edit dialog. One HTTP/UDP connection data-set
 * can be entered (in contrast to the edit dialog, which hides connections by default and allow to have more
 * than one connection)
 */
public class DevicesWizardNewDialog extends DialogFragment implements onCreateDeviceResult {
    private EditDeviceInterface editDevice = null;
    private ArrayAdapter<String> ip_autocomplete;
    private Toast toast;
    private AppData appData;

    public DevicesWizardNewDialog() {
    }

    public void setPlugin(AbstractBasePlugin abstractBasePlugin) {
        editDevice = abstractBasePlugin.openEditDevice(null);
        if (editDevice == null)
            throw new RuntimeException("DevicesWizardNewDialog only for configurable devices!");
        editDevice.setResultListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("pluginInterface", editDevice.getDevice().pluginID);
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // This should not happen, but could if activity is restored (low_mem etc)
        if (editDevice == null) {
            // Try to recover: TODO ListenService not ready here!!
//            String pluginID = savedInstanceState.getString("pluginInterface");
//            PluginInterface pluginInterface = ListenService.getService().getPlugin(pluginID);
//            // Recover failed
//            if (pluginInterface == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.device_add)
                    .setPositiveButton(android.R.string.ok, null);
            return builder.create();
//            }
//            setPlugin(pluginInterface);
        }

        appData = PluginService.getService().getAppData();

        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_new, null);

        ip_autocomplete = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        EditText textView;

        List<Device> devices = appData.deviceCollection.getItems();
        for (Device device : devices) {
            device.lockDevice();
            for (DeviceConnection deviceConnection : device.getDeviceConnections()) {
                ip_autocomplete.remove(deviceConnection.mHostName);
                ip_autocomplete.add(deviceConnection.mHostName);
            }
            device.releaseDevice();
        }

        AutoCompleteTextView iptext = (AutoCompleteTextView) view.findViewById(R.id.device_host);
        iptext.setAdapter(ip_autocomplete);

        textView = (EditText) view.findViewById(R.id.device_http_port);
        textView.setText("80");

        textView = (EditText) view.findViewById(R.id.device_udp_send);
        textView.setText(String.valueOf(SharedPrefs.getInstance().getDefaultSendPort()));

        textView = (EditText) view.findViewById(R.id.device_udp_receive);
        textView.setText(String.valueOf(SharedPrefs.getInstance().getDefaultReceivePort()));

        if (editDevice.getDevice() == null)
            throw new RuntimeException("DevicesWizardNewFragment: editDevice.getDevice not set");


        Device device = editDevice.getDevice();

        textView = (EditText) view.findViewById(R.id.device_username);
        textView.setText(device.getUserName());

        textView = (EditText) view.findViewById(R.id.device_password);
        textView.setText(device.getPassword());

        toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        view.findViewById(R.id.device_ip_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.device_ip_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });
        view.findViewById(R.id.device_username_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.device_username_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });
        view.findViewById(R.id.device_password_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.device_password_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
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
        device.lockDevice();

        device.makeTemporaryDevice();

        List<DeviceConnection> deviceConnectionList = new ArrayList<>();

        DeviceConnection deviceConnection = new DeviceConnectionHTTP(device, hostname, httpPort);
        deviceConnection.makeAssignedByDevice();
        deviceConnectionList.add(deviceConnection);

        deviceConnection = new DeviceConnectionUDP(device, hostname, udpReceive, udpSend);
        deviceConnection.makeAssignedByDevice();
        deviceConnectionList.add(deviceConnection);

        device.replaceAutomaticAssignedConnections(deviceConnectionList);

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        device.setUserName(textView.getText().toString());

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        device.setPassword(textView.getText().toString());

        device.releaseDevice();

        if (ip_autocomplete.getPosition(hostname) != -1) {
            Toast.makeText(getActivity(), R.string.device_already_exist, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!editDevice.startTest(PluginService.getService())) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void testFinished(EditDeviceInterface.TestStates state) {
        if (state == EditDeviceInterface.TestStates.TEST_OK) {
            appData.addToConfiguredDevicesFromOtherThread(editDevice.getDevice());
            dismiss();
        } else if (state == EditDeviceInterface.TestStates.TEST_ACCESS) {
            Toast.makeText(getActivity(), R.string.error_device_no_access, Toast.LENGTH_SHORT).show();
        } else if (state == EditDeviceInterface.TestStates.TEST_REACHABLE) {
            Toast.makeText(getActivity(), R.string.device_test_not_reachable, Toast.LENGTH_SHORT).show();
        }
    }
}