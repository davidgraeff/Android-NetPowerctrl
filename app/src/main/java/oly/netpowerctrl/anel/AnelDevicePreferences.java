package oly.netpowerctrl.anel;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;

public class AnelDevicePreferences extends DialogFragment implements AnelCreateDevice.AnelCreateDeviceResult {
    final AnelCreateDevice anelCreateDevice;

    public AnelDevicePreferences() {
        anelCreateDevice = ((AnelPlugin) NetpowerctrlService.getService().getPluginByID(AnelPlugin.PLUGIN_ID)).anelCreateDevice;
        anelCreateDevice.listener = this;
    }

    @Override
    public void onStop() {
        // Free objects within the AnelPlugin, that has been created for this dialog only
        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service != null)
            ((AnelPlugin) service.getPluginByID(AnelPlugin.PLUGIN_ID)).configureDeviceScreenClose();

        super.onStop();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_preferences, null);

        @SuppressLint("InflateParams")
        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_device_preferences_title, null);
        CheckBox checkBox = ((CheckBox) titleView.findViewById(android.R.id.title));
        checkBox.setText(R.string.device_enabled);
        checkBox.setChecked(anelCreateDevice.device.isEnabled());

        ((TextView) titleView.findViewById(R.id.device_name)).setText(anelCreateDevice.device.DeviceName);

        TextView textView;

        textView = (TextView) view.findViewById(R.id.device_name);
        textView.setText(anelCreateDevice.device.DeviceName);

        textView = (TextView) view.findViewById(R.id.device_username);
        textView.setText(anelCreateDevice.device.UserName);

        textView = (TextView) view.findViewById(R.id.device_password);
        textView.setText(anelCreateDevice.device.Password);

        textView = (TextView) view.findViewById(R.id.device_unique_id);
        textView.setText(anelCreateDevice.device.UniqueDeviceID);

        view.findViewById(R.id.device_name_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_name_summary, Toast.LENGTH_LONG).show();
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
        view.findViewById(R.id.device_additional_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.device_additional_connections_summary, Toast.LENGTH_LONG).show();
            }
        });

        checkBox = ((CheckBox) view.findViewById(R.id.show_additional_connections));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Toast.makeText(compoundButton.getContext(), "Kommt in der nächsten Version zurück", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.device_test, null)
                .setNegativeButton(android.R.string.cancel, null);
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

        textView = (TextView) getDialog().findViewById(R.id.device_name);
        anelCreateDevice.device.DeviceName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_username);
        anelCreateDevice.device.UserName = textView.getText().toString();

        textView = (TextView) getDialog().findViewById(R.id.device_password);
        anelCreateDevice.device.Password = textView.getText().toString();

        anelCreateDevice.device.setEnabled(((CheckBox) getDialog().findViewById(android.R.id.title)).isChecked());
    }

    private void testDevice() {
        storeValues();
        if (anelCreateDevice.isTesting())
            return;

        if (anelCreateDevice.device.UserName.isEmpty() || anelCreateDevice.device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (anelCreateDevice.device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!anelCreateDevice.startTest(getActivity())) {
            Toast.makeText(getActivity(), R.string.error_plugin_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDevice() {
        storeValues();
        if (anelCreateDevice.device.UserName.isEmpty() || anelCreateDevice.device.Password.isEmpty()) {
            Toast.makeText(getActivity(), R.string.create_device_not_all_data_set, Toast.LENGTH_SHORT).show();
            return;
        }

        if (anelCreateDevice.device.DeviceConnections.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return;
        }

        if (anelCreateDevice.test_state != AnelCreateDevice.TestStates.TEST_OK) {
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
        anelCreateDevice.wakeupPlugin(getActivity());

        RuntimeDataController.getDataController().addToConfiguredDevices(getActivity(), anelCreateDevice.device);
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void testFinished(boolean success) {
        if (!success) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getString(R.string.error_device_not_reachable) + ": " + anelCreateDevice.device.DeviceName + ":"
                            + Integer.valueOf(0).toString(),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void testDeviceNotReachable() {
        //noinspection ConstantConditions
        Toast.makeText(getActivity(),
                getString(R.string.error_device_no_access) + ": " + anelCreateDevice.device.UserName + " " + anelCreateDevice.device.Password,
                Toast.LENGTH_SHORT).show();
    }
}