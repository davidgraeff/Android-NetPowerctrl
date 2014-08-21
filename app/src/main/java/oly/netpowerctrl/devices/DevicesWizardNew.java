package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelCreateDevice;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Created by david on 20.08.14.
 */
public class DevicesWizardNew extends DialogFragment implements AnelCreateDevice.AnelCreateDeviceResult {
    AnelCreateDevice anelCreateDevice;

    public DevicesWizardNew() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_device_new, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_add_device)
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
        anelCreateDevice = new AnelCreateDevice(null, getActivity());
        anelCreateDevice.listener = this;

        //TODO set device data
        //anelCreateDevice.device;

        if (!anelCreateDevice.startTest()) {
            anelCreateDevice.listener = null;
            anelCreateDevice = null;
        }
    }

    @Override
    public void testFinished(boolean success) {
        if (success) {
            final Device deviceToAdd = anelCreateDevice.device;
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    NetpowerctrlApplication.getDataController().addToConfiguredDevices(deviceToAdd);
                }
            });
        } else {
        }
        anelCreateDevice.listener = null;
        anelCreateDevice = null;
    }

    @Override
    public void testDeviceNotReachable() {
        anelCreateDevice.listener = null;
        anelCreateDevice = null;
    }
}