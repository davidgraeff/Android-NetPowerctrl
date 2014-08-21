package oly.netpowerctrl.devices;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelCreateDevice;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Created by david on 20.08.14.
 */
public class DevicesAutomaticFragment extends DialogFragment implements AnelCreateDevice.AnelCreateDeviceResult {
    TextView textView;
    List<Device> deviceList;
    AnelCreateDevice anelCreateDevice;
    int current = 0;

    public DevicesAutomaticFragment() {
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        textView = new TextView(getActivity());
        textView.setText("Waiting for devices...\n");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.automaticConfiguration)
                .setView(textView)
                .setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        deviceList = new ArrayList<>(NetpowerctrlApplication.getDataController().newDevices);
        takeNext();
    }

    void takeNext() {
        if (current >= deviceList.size()) {
            textView.append("DONE");
            return;
        }
        Device device = deviceList.get(current);
        ++current;
        textView.append("Check " + device.DeviceName + "...\n");

        anelCreateDevice = new AnelCreateDevice(device, getActivity());
        anelCreateDevice.listener = this;
        if (!anelCreateDevice.startTest()) {
            textView.append("\tPlugin failed\n");
            anelCreateDevice.listener = null;
            anelCreateDevice = null;
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    takeNext();
                }
            });
        }
    }

    @Override
    public void testFinished(boolean success) {
        if (success) {
            textView.append("\tOK\n");
            final Device deviceToAdd = anelCreateDevice.device;
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    NetpowerctrlApplication.getDataController().addToConfiguredDevices(deviceToAdd);
                }
            });
        } else {
            textView.append("\tFAILED\n");
        }
        anelCreateDevice.listener = null;
        anelCreateDevice = null;
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                takeNext();
            }
        });
    }

    @Override
    public void testDeviceNotReachable() {
        anelCreateDevice.listener = null;
        anelCreateDevice = null;
        textView.append("\tLogin data wrong\n");
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                takeNext();
            }
        });
    }
}
