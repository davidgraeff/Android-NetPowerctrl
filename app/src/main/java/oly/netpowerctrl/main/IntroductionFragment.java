package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.devices.onCreateDeviceResult;
import oly.netpowerctrl.pluginservice.AbstractBasePlugin;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class IntroductionFragment extends Fragment implements onCreateDeviceResult {
    Handler takeNextHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            takeNext();
        }
    };
    private TextView textView;
    private List<Device> deviceList;
    private EditDeviceInterface editDevice;
    private int current = 0;
    private AppData appData = null;

    public IntroductionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.automatic_intro, null);

        textView = (TextView) root.findViewById(R.id.text_automatic);

        Button button = (Button) root.findViewById(R.id.btnStart);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        button = (Button) root.findViewById(R.id.btnClose);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
            }
        });

        button = (Button) root.findViewById(R.id.btnDevices);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.instance.getSupportActionBar().hide();
        if (MainActivity.getNavigationController().getDrawerLayout() != null) // tablet landscape mode
            MainActivity.getNavigationController().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity.instance.getSupportActionBar().show();
        if (MainActivity.getNavigationController().getDrawerLayout() != null) // tablet landscape mode
            MainActivity.getNavigationController().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    void start() {
        textView.setText("Waiting for devices...\n");
        appData = PluginService.getService().getAppData();
        if (appData == null) return;
        deviceList = new ArrayList<>(appData.unconfiguredDeviceCollection.getItems());
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
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        editDevice = ((AbstractBasePlugin) device.getPluginInterface()).openEditDevice(device);

        // If no edit device -> there is no configuration necessary
        if (editDevice == null) {
            textView.append("\tOK\n");
            appData.addToConfiguredDevices(device);
            editDevice = null;
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        editDevice.setResultListener(this);

        if (!editDevice.startTest(PluginService.getService())) {
            textView.append("\tPlugin failed\n");
            editDevice = null;
            takeNextHandler.sendEmptyMessage(0);
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
                    // Add to group with name DeviceName
                    int index = appData.groupCollection.add(deviceToAdd.getDeviceName());
                    UUID group = appData.groupCollection.get(index).uuid;
                    deviceToAdd.lockDevicePorts();
                    Iterator<DevicePort> devicePortIterator = deviceToAdd.getDevicePortIterator();
                    while (devicePortIterator.hasNext())
                        devicePortIterator.next().groups.add(group);
                    deviceToAdd.releaseDevicePorts();
                    // Add device to configured devices
                    appData.addToConfiguredDevices(deviceToAdd);
                }
            });
        } else {
            textView.append("\tFAILED\n");
        }
        editDevice = null;
        takeNextHandler.sendEmptyMessage(0);
    }

    @Override
    public void testDeviceNotReachable() {
        editDevice = null;
        textView.append("\tLogin data wrong\n");
        takeNextHandler.sendEmptyMessage(0);
    }
}
