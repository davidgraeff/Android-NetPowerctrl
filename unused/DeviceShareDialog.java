package oly.netpowerctrl.credentials;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import oly.netpowerctrl.R;
;
import oly.netpowerctrl.credentials.device.Device;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.ui.FragmentUtils;

/**
 * Share a device to network dialog. This will be shown if a user clicks on a configured device
 * in the device screen and within the context menu he choose "Share to network".
 * This dialog allows the user to setup a username and password for the share and enable/disable it.
 */
public class DeviceShareDialog extends DialogFragment {
    private Device device;

    public DeviceShareDialog() {
    }

    public static void show(Activity context, Device device) {
        DeviceShareDialog newFragment = (DeviceShareDialog) Fragment.instantiate(context, DeviceShareDialog.class.getName());
        Bundle bundle = new Bundle();
        bundle.putString("device", device.getUniqueDeviceID());
        newFragment.setArguments(bundle);
        FragmentUtils.changeToDialog(context, newFragment);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
         PluginService pluginService = PluginService.getService();
        if (bundle != null)
            device = PluginService.findDevice(bundle.getString("device"));
        if (device == null && savedInstanceState != null)
            device = PluginService.findDevice(savedInstanceState.getString("device"));

        if (device != null) {
            @SuppressLint("InflateParams")
            final View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_share_network, null);
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            }).setTitle(R.string.device_share_to_network).setView(rootView).create();
        } else { // not found anymore
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            }).setTitle(R.string.error_device_not_found).create();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (device != null)
            outState.putString("device", device.getUniqueDeviceID());
        super.onSaveInstanceState(outState);
    }
}
