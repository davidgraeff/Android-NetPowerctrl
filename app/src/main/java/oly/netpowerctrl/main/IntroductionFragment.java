package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.devices.onCreateDeviceResult;
import oly.netpowerctrl.pluginservice.AbstractBasePlugin;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class IntroductionFragment extends Fragment implements onCreateDeviceResult {
    private Handler takeNextHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            takeNext();
        }
    };
    private LinearLayout found_devices_layout;
    private TextView find_device_status;
    private List<Device> deviceList;
    private EditDeviceInterface editDevice;
    private int current = 0;
    private AppData appData = null;
    private View item;

    public IntroductionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.automatic_intro, null);

        found_devices_layout = (LinearLayout) root.findViewById(R.id.automatic_devices);
        find_device_status = (TextView) root.findViewById(R.id.automatic_status);

        Button button = (Button) root.findViewById(R.id.btnRefresh);
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
                getActivity().finish();
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.instance.getSupportActionBar().hide();
        start();
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity.instance.getSupportActionBar().show();
    }

    void start() {
        find_device_status.setVisibility(View.VISIBLE);
        find_device_status.setText("Suche Geräte...");
        found_devices_layout.removeAllViews();
        appData = PluginService.getService().getAppData();
        if (appData == null) return;
        deviceList = new ArrayList<>(appData.unconfiguredDeviceCollection.getItems());
        takeNext();
    }

    void takeNext() {
        LayoutInflater l = LayoutInflater.from(getActivity());
        item = l.inflate(R.layout.list_item_device, found_devices_layout);

        if (current >= deviceList.size()) {
            if (current == 0)
                find_device_status.setText("Keine Geräte gefunden. Im Gerätebildschirm können Geräte per Hand hinzugefügt werden.");
            else
                find_device_status.setVisibility(View.GONE);
            return;
        }
        Device device = deviceList.get(current);

        item.findViewById(R.id.device_connection_reachable).setVisibility(View.GONE);

        ++current;
        ((TextView) item.findViewById(R.id.title)).setText(device.getDeviceName());

        if (device.getPluginInterface() == null) {
            ((TextView) item.findViewById(R.id.subtitle)).append(getString(R.string.error_plugin_not_installed));
            editDevice = null;
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        editDevice = ((AbstractBasePlugin) device.getPluginInterface()).openEditDevice(device);

        // If no edit device -> there is no configuration necessary
        if (editDevice == null) {
            ((TextView) item.findViewById(R.id.subtitle)).append(getString(android.R.string.ok));
            appData.addToConfiguredDevices(device);
            editDevice = null;
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        editDevice.setResultListener(this);

        ((TextView) item.findViewById(R.id.subtitle)).append(getString(R.string.device_connection_testing));

        if (!editDevice.startTest(PluginService.getService())) {
            ((TextView) item.findViewById(R.id.subtitle)).append(getString(R.string.error_plugin_failed));
            editDevice = null;
            takeNextHandler.sendEmptyMessage(0);
        }
    }

    @Override
    public void testFinished(boolean success) {
        if (success) {
            ((TextView) item.findViewById(R.id.subtitle)).append(getString(android.R.string.ok));
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
            ((TextView) item.findViewById(R.id.subtitle)).append(getString(R.string.error_device_not_found));
        }
        editDevice = null;
        takeNextHandler.sendEmptyMessage(0);
    }

    @Override
    public void testDeviceNotReachable() {
        editDevice = null;
        ((TextView) item.findViewById(R.id.subtitle)).append(getString(R.string.error_device_no_access));
        takeNextHandler.sendEmptyMessage(0);
    }
}
