package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
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
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.MainActivity;

public class DeviceEditFragmentDialog extends DialogFragment implements onCreateDeviceResult {
    final List<View> connectionViews = new ArrayList<>();
    private EditDeviceInterface editDevice = null;
    private Button btnAddHttp;
    private ImageButton btnAddHttpHelp;
    private RelativeLayout layout;
    private CheckBox chkAdditionalConnections;

    public DeviceEditFragmentDialog() {
    }

    public void setDevice(Device device) {
        PluginInterface pluginInterface = ListenService.getService().getPluginByID(device.pluginID);
        if (pluginInterface == null)
            return;

        editDevice = pluginInterface.openEditDevice(device);
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

        ((TextView) titleView.findViewById(R.id.device_name)).setText(device.DeviceName);

        TextView textView;

        textView = (TextView) rootView.findViewById(R.id.device_name);
        textView.setText(device.DeviceName);

        textView = (TextView) rootView.findViewById(R.id.device_username);
        textView.setText(device.UserName);

        textView = (TextView) rootView.findViewById(R.id.device_password);
        textView.setText(device.Password);

        textView = (TextView) rootView.findViewById(R.id.device_unique_id);
        textView.setText(device.UniqueDeviceID);

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
                Iterator<DeviceConnection> deviceConnectionIterator = device.DeviceConnections.iterator();
                while (deviceConnectionIterator.hasNext())
                    if (deviceConnectionIterator.next().isAssignedByDevice())
                        deviceConnectionIterator.remove();
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
                DeviceEditNewHttpDialog dialog = (DeviceEditNewHttpDialog) Fragment.instantiate(getActivity(), DeviceEditNewHttpDialog.class.getName());
                dialog.setDevice(device);
                MainActivity.getNavigationController().changeToDialog(getActivity(), dialog);
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
        List<DeviceConnection> connections = device.DeviceConnections;

        int assignedConnections = 0;

        for (DeviceConnection connection1 : connections) {
            if (connection1.isAssignedByDevice()) {
                ++assignedConnections;
                continue;
            }

            if (!(connection1 instanceof DeviceConnectionHTTP))
                continue;

            final DeviceConnectionHTTP connection = (DeviceConnectionHTTP) connection1;

            View p = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_list_connection_http, layout, false);
            ((EditText) p.findViewById(R.id.device_host)).setText(connection.mHostName);
            ((EditText) p.findViewById(R.id.device_http_port)).setText(String.valueOf(connection.getDestinationPort()));
            ImageView imageView = ((ImageView) p.findViewById(R.id.connection_reachable));
            imageView.setVisibility(View.VISIBLE);
            if (connection.isReachable())
                imageView.setImageResource(android.R.drawable.presence_online);
            else
                imageView.setImageResource(android.R.drawable.presence_offline);
            p.findViewById(R.id.connection_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    device.removeConnection(connection);
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

        textView = (TextView) getDialog().findViewById(R.id.device_name);
        device.DeviceName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        device.UserName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        device.Password = textView.getText().toString();

        device.setEnabled(((CheckBox) getDialog().findViewById(android.R.id.title)).isChecked());
    }

    private void testDevice() {
        storeValues();
        if (editDevice.isTesting())
            return;

        Device device = editDevice.getDevice();

        if (device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!editDevice.startTest(getActivity())) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDevice() {
        storeValues();

        Device device = editDevice.getDevice();

        if (device.UserName.isEmpty() || device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!editDevice.isTestOK()) {
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
        editDevice.wakeupPlugin(getActivity());

        AppData.getInstance().addToConfiguredDevices(getActivity(), editDevice.getDevice());
        dismiss();
    }

    @Override
    public void testFinished(boolean success) {
        if (!success) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getString(R.string.error_device_not_reachable) + ": " + editDevice.getDevice().DeviceName + ":"
                            + Integer.valueOf(0).toString(),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();

            updateConnections();
        }
    }

    @Override
    public void testDeviceNotReachable() {
        //noinspection ConstantConditions
        Toast.makeText(getActivity(),
                getString(R.string.error_device_no_access) + ": " + editDevice.getDevice().UserName + " " + editDevice.getDevice().Password,
                Toast.LENGTH_SHORT).show();
    }
}