package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionHTTP;
import oly.netpowerctrl.device_base.executables.ExecutableReachability;
import oly.netpowerctrl.pluginservice.DeviceQuery;
import oly.netpowerctrl.pluginservice.PluginService;

public class DeviceEditDialog extends DialogFragment implements onCreateDeviceResult {
    final List<View> connectionViews = new ArrayList<>();
    private EditDeviceInterface editDevice = null;
    private Button btnAddHttp;
    private ImageButton btnAddHttpHelp;
    private RelativeLayout layout;
    private CheckBox chkAdditionalConnections;

    public DeviceEditDialog() {
    }

    public void setDevice(@NonNull Device device) {
        editDevice = PluginService.getService().openEditDevice(device);
        editDevice.setResultListener(this);
    }


    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (editDevice == null)
            return null;

        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_edit, null);
        layout = (RelativeLayout) rootView.findViewById(R.id.layout);

        final Device device = editDevice.getDevice();

        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_title_device_edit, null);
        CheckBox checkBox = ((CheckBox) titleView.findViewById(android.R.id.title));
        checkBox.setText(R.string.device_enabled);
        checkBox.setChecked(device.isEnabled());

        ((TextView) titleView.findViewById(R.id.device_name)).setText(device.getDeviceName());

        TextView textView;

        textView = (TextView) rootView.findViewById(R.id.device_name);
        textView.setText(device.getDeviceName());

        textView = (TextView) rootView.findViewById(R.id.device_username);
        textView.setText(device.getUserName());

        textView = (TextView) rootView.findViewById(R.id.device_password);
        textView.setText(device.getPassword());

        textView = (TextView) rootView.findViewById(R.id.device_unique_id);
        textView.setText(device.getUniqueDeviceID());

        rootView.findViewById(R.id.device_name_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_name_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.device_username_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_username_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.device_password_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_password_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.device_additional_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_additional_connections_summary, Toast.LENGTH_LONG).show();
            }
        });

        // Remove automatically assigned connections
        rootView.findViewById(R.id.connection_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device.lockDevice();
                Iterator<DeviceConnection> deviceConnectionIterator = device.getDeviceConnections().iterator();
                while (deviceConnectionIterator.hasNext())
                    if (deviceConnectionIterator.next().isAssignedByDevice())
                        deviceConnectionIterator.remove();
                device.releaseDevice();
                updateConnections();
            }
        });

        chkAdditionalConnections = ((CheckBox) rootView.findViewById(R.id.show_additional_connections));
        chkAdditionalConnections.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showConnections(b);
            }
        });

        // Add connection
        btnAddHttp = (Button) rootView.findViewById(R.id.device_add_http);
        btnAddHttp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceEditNewHttpDialog.show(getActivity(), device);
            }
        });
        btnAddHttpHelp = (ImageButton) rootView.findViewById(R.id.device_new_http_help_icon);
        btnAddHttpHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_http_new_summary, Toast.LENGTH_LONG).show();
            }
        });

        showConnections(false);

        updateConnections();

        // Build dialog

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.device_test, null)
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    private void updateConnections() {
        // Clear existing
        for (View view : connectionViews) {
            layout.removeView(view);
        }
        connectionViews.clear();

        boolean showViews = chkAdditionalConnections.isChecked();

        // List of connections
        int lastID = R.id.device_add_http;

        final Device device = editDevice.getDevice();
        device.lockDevice();
        List<DeviceConnection> connections = new ArrayList<>(device.getDeviceConnections());
        device.releaseDevice();

        int assignedConnections = 0;

        for (int connection_id = 0; connection_id < connections.size(); ++connection_id) {
            DeviceConnection deviceConnection = connections.get(connection_id);
            if (deviceConnection.isAssignedByDevice()) {
                ++assignedConnections;
                continue;
            }

            if (!(deviceConnection instanceof DeviceConnectionHTTP))
                continue;

            final DeviceConnectionHTTP connection = (DeviceConnectionHTTP) deviceConnection;

            View p = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_list_connection_http, layout, false);
            ((EditText) p.findViewById(R.id.device_host)).setText(connection.mHostName);
            ((EditText) p.findViewById(R.id.device_http_port)).setText(String.valueOf(connection.getDestinationPort()));
            ImageView imageView = ((ImageView) p.findViewById(R.id.connection_reachable));
            imageView.setVisibility(View.VISIBLE);
            if (connection.reachableState() != ExecutableReachability.NotReachable)
                imageView.setImageResource(android.R.drawable.presence_online);
            else
                imageView.setImageResource(android.R.drawable.presence_offline);
            final int finalConnection_id = connection_id;
            p.findViewById(R.id.connection_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    device.removeConnection(finalConnection_id);
                    updateConnections();
                }
            });
            p.setVisibility(showViews ? View.VISIBLE : View.GONE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.BELOW, lastID);
            p.setId(lastID + 1);
            ++lastID;
            layout.addView(p, lp);
            connectionViews.add(p);
        }

        ((TextView) layout.findViewById(R.id.assigned_connections)).setText(getString(R.string.device_assignedConnections, assignedConnections));
    }

    private void showConnections(boolean b) {
        final int v = b ? View.VISIBLE : View.GONE;
        btnAddHttp.setVisibility(v);
        btnAddHttpHelp.setVisibility(v);
        for (View view : connectionViews)
            view.setVisibility(v);
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        Button button = d.getButton(Dialog.BUTTON_POSITIVE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDevice();
            }
        });
        button = d.getButton(Dialog.BUTTON_NEUTRAL);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDevice();
            }
        });
        button = d.getButton(Dialog.BUTTON_NEGATIVE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void storeValues() {
        TextView textView;

        Device device = editDevice.getDevice();
        device.lockDevice();

        textView = (TextView) getDialog().findViewById(R.id.device_name);
        device.setDeviceName(textView.getText().toString());

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        device.setUserName(textView.getText().toString());

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        device.setPassword(textView.getText().toString());

        device.setEnabled(((CheckBox) getDialog().findViewById(android.R.id.title)).isChecked());
        device.releaseDevice();
    }

    private void testDevice() {
        storeValues();
        if (editDevice.getTestState() != EditDeviceInterface.TestStates.TEST_INIT)
            return;

        Device device = editDevice.getDevice();

        if (device.getUserName().isEmpty() || device.getPassword().isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.hasNoConnections()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!editDevice.startTest(PluginService.getService())) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDevice() {
        storeValues();

        Device device = editDevice.getDevice();

        if (device.getUserName().isEmpty() || device.getPassword().isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.hasNoConnections()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (editDevice.getTestState() != EditDeviceInterface.TestStates.TEST_OK) {
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
        Device device = editDevice.getDevice();
        PluginService service = PluginService.getService();
        service.getAppData().addToConfiguredDevices(device);
        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (device.getFirstReachableConnection() == null)
            new DeviceQuery(service, null, device);
        dismiss();
    }

    @Override
    public void testFinished(EditDeviceInterface.TestStates state) {
        if (state == EditDeviceInterface.TestStates.TEST_OK) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();

            updateConnections();
        } else if (state == EditDeviceInterface.TestStates.TEST_ACCESS) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getString(R.string.error_device_no_access) + ": " + editDevice.getDevice().getUserName() + " " + editDevice.getDevice().getPassword(),
                    Toast.LENGTH_SHORT).show();
        } else if (state == EditDeviceInterface.TestStates.TEST_REACHABLE) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getString(R.string.device_test_not_reachable) + ": " + editDevice.getDevice().getDeviceName() + ":"
                            + Integer.valueOf(0).toString(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}