package oly.netpowerctrl.ioconnection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.UnknownHostException;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.ui.FragmentUtils;

;

/**
 * This dialog allows the user to setup a new
 */
public class IOConnectionHttpDialog extends DialogFragment {
    private ProgressBar progressBar;
    private EditText newHost;
    private EditText newPort;
    private ImageView connectionStateImage;
    private boolean closeIfReachable = false;
    private IOConnectionHTTP ioConnection = null;

    public IOConnectionHttpDialog() {
    }

    public static void show(Activity context, IOConnectionHTTP ioConnection) {
        IOConnectionHttpDialog dialog = (IOConnectionHttpDialog) Fragment.instantiate(context, IOConnectionHttpDialog.class.getName());
        dialog.ioConnection = ioConnection;
        FragmentUtils.changeToDialog(context, dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_edit_connection_http, null);
        rootView.findViewById(R.id.connection_delete).setVisibility(View.GONE);

        //noinspection ConstantConditions
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.outlet_edit_title, ioConnection.credentials.getDeviceName()));
        //builder.setMessage(getString(R.string.outlet_rename_message, device.getTitle()));

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
        final String host = newHost.getText().toString().trim();

        final int port;
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

        IOConnectionHTTP test_connection = new IOConnectionHTTP();
        test_connection.copyFrom(ioConnection);
        test_connection.connectionUID = UUID.randomUUID().toString();
        test_connection.hostName = host;
        test_connection.PortHttp = port;

        new AsyncTask<IOConnectionHTTP, Void, IOConnectionHTTP>() {
            private String error_message = null;
            private boolean success = false;

            @Override
            protected void onPreExecute() {
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected IOConnectionHTTP doInBackground(IOConnectionHTTP... connections) {
                try {
                    connections[0].lookupIPs();
                    HttpThreadPool.HTTPRunner<Void> h = new HttpThreadPool.HTTPRunner<>(connections[0], "", "", null, false, new HttpThreadPool.HTTPCallback<Void>() {
                        @Override
                        public void httpResponse(Void additional, boolean callback_success, String response_message) {
                            success = callback_success;
                            error_message = response_message;
                        }
                    });
                    h.run();
                } catch (UnknownHostException e) {
                    error_message = e.getLocalizedMessage();
                    return null;
                }
                return connections[0];
            }

            @Override
            protected void onPostExecute(IOConnectionHTTP testConnection) {
                progressBar.setVisibility(View.INVISIBLE);

                if (success) {
                    ioConnection.copyFrom(testConnection);
                    ioConnection.incReceivedPackets();
                    if (closeIfReachable) {
                        DataService dataService = DataService.getService();
                        dataService.connections.put(ioConnection);
                        dismiss();
                    } else {
                        connectionStateImage.setImageResource(android.R.drawable.presence_online);
                    }
                } else {
                    connectionStateImage.setImageResource(android.R.drawable.presence_offline);
                    Toast.makeText(App.instance, App.instance.getString(R.string.device_notReachable, error_message), Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(test_connection);
    }
}
