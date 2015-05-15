package oly.netpowerctrl.devices;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.rey.material.app.Dialog;
import com.rey.material.widget.EditText;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 * Create/Edit credentials of a device. This can only be done after a first contact to the destination
 * device has been established and an IOConnection is available.
 */
public class CredentialsDialog extends DialogFragment {
    private Credentials credentials;

    public CredentialsDialog() {
    }

    public void setCredentials(AbstractBasePlugin plugin, Credentials credentials) {
        this.credentials = credentials;
        if (this.credentials == null)
            this.credentials = plugin.createNewDefaultCredentials();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_credentials, container, false);
        // This should not happen, but could if activity is restored (low_mem etc)
        if (credentials == null) {
            return view;
        }

        EditText textView;

        textView = (EditText) view.findViewById(R.id.device_name);
        textView.setText(credentials.deviceName);

        textView = (EditText) view.findViewById(R.id.device_username);
        textView.setText(credentials.userName);

        textView = (EditText) view.findViewById(R.id.device_password);
        textView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        textView.setText(credentials.password);

        CompoundButton c = (CompoundButton) view.findViewById(R.id.show_password);
        c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                EditText textView = (EditText) view.findViewById(R.id.device_password);
                if (b)
                    textView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                else
                    textView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), ThemeHelper.getDialogRes(getActivity()));
        dialog.setTitle(credentials.isConfigured() ? R.string.device_edit_credentials : R.string.device_add);
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText textView;
                String deviceName, userName, password;

                textView = (EditText) getDialog().findViewById(R.id.device_name);
                deviceName = (textView.getText().toString());
                textView = (EditText) getDialog().findViewById(R.id.device_username);
                userName = (textView.getText().toString());
                textView = (EditText) getDialog().findViewById(R.id.device_password);
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
        }).positiveAction(android.R.string.ok);
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        }).negativeAction(android.R.string.cancel);
        return dialog;
    }

}