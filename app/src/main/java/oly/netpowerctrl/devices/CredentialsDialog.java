package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

;

/**
 * Create/Edit credentials of a device. This can only be done after a first contact to the destination
 * device has been established and an IOConnection is available.
 */
public class CredentialsDialog extends DialogFragment {
    private Credentials credentials;
    private Toast toast;

    public CredentialsDialog() {
    }

    public void setCredentials(AbstractBasePlugin plugin, Credentials credentials) {
        this.credentials = credentials;
        if (this.credentials == null)
            this.credentials = plugin.createNewDefaultCredentials();
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // This should not happen, but could if activity is restored (low_mem etc)
        if (credentials == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.device_add).setPositiveButton(android.R.string.ok, null);
            return builder.create();
        }

        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_credentials, null);

        EditText textView;

        textView = (EditText) view.findViewById(R.id.device_name);
        textView.setText(credentials.deviceName);

        textView = (EditText) view.findViewById(R.id.device_username);
        textView.setText(credentials.userName);

        textView = (EditText) view.findViewById(R.id.device_password);
        textView.setText(credentials.password);

        toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        view.findViewById(R.id.device_name_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.device_name_summary);
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
        builder.setTitle(credentials.isConfigured() ? R.string.device_edit_credentials : R.string.device_add)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
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
                TextView textView;
                String deviceName, userName, password;

                textView = (TextView) getDialog().findViewById(R.id.device_name);
                deviceName = (textView.getText().toString());
                textView = (TextView) getDialog().findViewById(R.id.device_username);
                userName = (textView.getText().toString());
                textView = (TextView) getDialog().findViewById(R.id.device_password);
                password = (textView.getText().toString());

                if (deviceName.trim().length() == 0 || userName.trim().length() == 0 || password.trim().length() == 0) {
                    Toast.makeText(getActivity(), R.string.error_device_incomplete, Toast.LENGTH_SHORT).show();
                    return;
                }

                credentials.deviceName = deviceName;
                credentials.userName = userName;
                credentials.password = password;

                DataService.getService().addToConfiguredDevices(credentials);
                dismiss();
            }
        });
    }
}