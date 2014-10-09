package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.UnknownHostException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;

public class DeviceEditNewHttpDialog extends DialogFragment {
    private Device device;
    private ProgressBar progressBar;
    private EditText newHost;
    private EditText newPort;
    private ImageView connectionStateImage;
    private boolean closeIfReachable = false;
    private DeviceConnectionHTTP newDeviceConnection = null;

    public DeviceEditNewHttpDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_edit_connection_http, null);
        rootView.findViewById(R.id.connection_delete).setVisibility(View.GONE);

        //noinspection ConstantConditions
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.outlet_edit_title, device.DeviceName));
        //builder.setMessage(getString(R.string.outlet_rename_message, device.getDescription()));

        progressBar = (ProgressBar) rootView.findViewById(R.id.connection_progressbar);
        newHost = (EditText) rootView.findViewById(R.id.device_host);
        newPort = (EditText) rootView.findViewById(R.id.device_http_port);
        connectionStateImage = ((ImageView) rootView.findViewById(R.id.connection_reachable));
        connectionStateImage.setVisibility(View.VISIBLE);
        connectionStateImage.setImageResource(android.R.drawable.presence_offline);

        builder.setView(rootView);
        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.setNeutralButton(getString(R.string.device_test), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        d.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeIfReachable = false;
                checkConnectionReachable();

            }
        });
        d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeIfReachable = true;
                checkConnectionReachable();
            }
        });
    }

    private void checkConnectionReachable() {
        String host = newHost.getText().toString().trim();

        int port;
        try {
            port = Integer.valueOf(newPort.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), R.string.error_invalid_port, Toast.LENGTH_SHORT).show();
            return;
        }

        if (host.trim().isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_invalid_host, Toast.LENGTH_SHORT).show();
            return;
        }

        newDeviceConnection = new DeviceConnectionHTTP(device, host, port);

        new AsyncTask<Void, Void, Boolean>() {
            private String error_message = null;

            @Override
            protected void onPreExecute() {
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    newDeviceConnection.lookupIPs();
                } catch (UnknownHostException e) {
                    error_message = e.getLocalizedMessage();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                progressBar.setVisibility(View.INVISIBLE);

                if (success) {
                    if (closeIfReachable) {
                        device.addConnection(newDeviceConnection);
                        dismiss();
                    } else {
                        connectionStateImage.setImageResource(android.R.drawable.presence_online);
                    }
                } else {
                    connectionStateImage.setImageResource(android.R.drawable.presence_offline);
                    Toast.makeText(App.instance, App.instance.getString(R.string.device_notReachable, error_message), Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    public void setDevice(Device devicePort) {
        this.device = devicePort;
    }
}
