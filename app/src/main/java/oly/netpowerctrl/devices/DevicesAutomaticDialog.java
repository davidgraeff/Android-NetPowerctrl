package oly.netpowerctrl.devices;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class DevicesAutomaticDialog extends DialogFragment implements onCreateDeviceResult {
    private TextView textView;
    private List<Device> deviceList;
    private EditDeviceInterface editDevice;
    private int current = 0;

    public DevicesAutomaticDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        textView = new TextView(getActivity());
        textView.setText("Waiting for devices...\n");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.automaticConfiguration)
                .setView(textView)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.devices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
                    }
                });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        deviceList = new ArrayList<>(AppData.getInstance().unconfiguredDeviceCollection.getItems());
        takeNext();
    }

    void takeNext() {
        if (current >= deviceList.size()) {
            textView.append("DONE");
            return;
        }
        Device device = deviceList.get(current);

        ++current;
        textView.append("Check " + device.getDeviceName() + "...\n");

        if (device.getPluginInterface() == null) {
            textView.append("\tPlugin not found\n");
            editDevice = null;
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    takeNext();
                }
            });
            return;
        }

        editDevice = ((PluginInterface) device.getPluginInterface()).openEditDevice(device);

        // If no edit device -> there is no configuration necessary
        if (editDevice == null) {
            textView.append("\tOK\n");
            AppData.getInstance().addToConfiguredDevices(getActivity(), device);
            editDevice = null;
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    takeNext();
                }
            });
            return;
        }

        editDevice.setResultListener(this);

        if (!editDevice.startTest(getActivity())) {
            textView.append("\tPlugin failed\n");
            editDevice = null;
            App.getMainThreadHandler().post(new Runnable() {
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
            final Device deviceToAdd = editDevice.getDevice();
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    AppData d = AppData.getInstance();
                    // Add to group with name DeviceName
                    int index = d.groupCollection.add(deviceToAdd.getDeviceName());
                    UUID group = d.groupCollection.get(index).uuid;
                    deviceToAdd.lockDevicePorts();
                    Iterator<DevicePort> devicePortIterator = deviceToAdd.getDevicePortIterator();
                    while (devicePortIterator.hasNext())
                        devicePortIterator.next().groups.add(group);
                    deviceToAdd.releaseDevicePorts();
                    // Add device to configured devices
                    d.addToConfiguredDevices(getActivity(), deviceToAdd);
                }
            });
        } else {
            textView.append("\tFAILED\n");
        }
        editDevice = null;
        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                takeNext();
            }
        });
    }

    @Override
    public void testDeviceNotReachable() {
        editDevice = null;
        textView.append("\tLogin data wrong\n");
        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                takeNext();
            }
        });
    }
}
