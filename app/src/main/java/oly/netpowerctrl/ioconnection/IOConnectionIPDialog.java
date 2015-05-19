package oly.netpowerctrl.ioconnection;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;
import com.rey.material.widget.EditText;
import com.rey.material.widget.ProgressView;

import java.util.UUID;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.adapter.IOConnectionIP;
import oly.netpowerctrl.plugin_wol.MagicPacket;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 * This dialog allows the user to setup a new
 */
public class IOConnectionIPDialog extends DialogFragment {
    private ProgressView progressView;
    private Button btnTest;
    private EditText newHost;
    private ImageView connectionStateImage;
    private boolean closeIfReachable = false;
    private IOConnectionIP ioConnection = null;

    public IOConnectionIPDialog() {
    }

    public static void show(Activity context, IOConnectionIP ioConnection) {
        IOConnectionIPDialog dialog = (IOConnectionIPDialog) Fragment.instantiate(context, IOConnectionIPDialog.class.getName());
        dialog.ioConnection = ioConnection;
        FragmentUtils.changeToDialog(context, dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_device_edit_connection_ip, container, false);
        progressView = (ProgressView) rootView.findViewById(R.id.connection_progressbar);
        btnTest = (Button) rootView.findViewById(R.id.btnTest);
        newHost = (EditText) rootView.findViewById(R.id.device_host);
        newHost.setText(ioConnection.hostName);
        connectionStateImage = ((ImageView) rootView.findViewById(R.id.connection_reachable));
        connectionStateImage.setVisibility(View.VISIBLE);
        connectionStateImage.setImageResource(android.R.drawable.presence_offline);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeIfReachable = false;
                checkConnectionReachable();
            }
        });

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SimpleDialog.Builder builder = new SimpleDialog.Builder(ThemeHelper.getDialogRes(getActivity()));
        builder.title(getString(R.string.outlet_edit_title, ioConnection.credentials.getDeviceName()));
        builder.negativeAction(getString(android.R.string.cancel))
                .positiveAction(getString(R.string.save));

        final Dialog dialog = builder.build(getActivity());
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeIfReachable = true;
                checkConnectionReachable();
            }
        });
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        return dialog;
    }


    private void checkConnectionReachable() {
        final String host = newHost.getText().toString().trim();

        if (host.trim().isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_invalid_host, Toast.LENGTH_SHORT).show();
            return;
        }

        IOConnectionIP test_connection = new IOConnectionIP();
        test_connection.copyFrom(ioConnection);
        test_connection.connectionUID = UUID.randomUUID().toString();
        test_connection.hostName = host;

        new AsyncTask<IOConnectionIP, Void, IOConnectionIP>() {
            private String error_message = null;
            private boolean success = false;

            @Override
            protected void onPreExecute() {
                progressView.start();
                btnTest.setEnabled(false);
            }

            @Override
            protected IOConnectionIP doInBackground(IOConnectionIP... connections) {
                if (!MagicPacket.doPing(connections[0].getDestinationHost())) return null;
                String mac = MagicPacket.getMacFromArpCache(connections[0].getDestinationHost());
                if (mac == null) return null;
                connections[0].additional = mac;
                success = true;
                return connections[0];
            }

            @Override
            protected void onPostExecute(IOConnectionIP testConnection) {
                progressView.stop();
                btnTest.setEnabled(true);

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
