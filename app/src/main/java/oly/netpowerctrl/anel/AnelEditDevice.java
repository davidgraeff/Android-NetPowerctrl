package oly.netpowerctrl.anel;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.devices.onCreateDeviceResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Use this class for testing device settings. Results are propagated via the onCreateDeviceResult interface.
 * The EditDeviceInterface is also implemented so this can be returned by the anel plugin for editing devices.
 */
public class AnelEditDevice implements onDeviceObserverResult, onCollectionUpdated<DeviceCollection, Device>, EditDeviceInterface {
    TestStates test_state = TestStates.TEST_INIT;
    private Device device;
    private onCreateDeviceResult listener = null;
    private Handler timeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (test_state == TestStates.TEST_ACCESS || test_state == TestStates.TEST_INIT) {
                test_state = TestStates.TEST_INIT;
                if (listener != null)
                    listener.testDeviceNotReachable();
            }
        }
    };
    private DeviceQuery deviceQuery;

    public AnelEditDevice(String defaultDeviceName, @Nullable Device edit_device) {
        if (edit_device == null) {
            device = new Device(AnelUDPReceive.anelPlugin.getPluginID(), true);
            device.setDeviceName(defaultDeviceName);
            // Default values for user and password
            device.setUserName("admin");
            device.setPassword("anel");
        } else {
            device = edit_device;
        }
    }

    @Override
    public void onObserverDeviceUpdated(Device updated_device) {
        updated(null, updated_device, ObserverUpdateActions.UpdateAction, -1);
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        if (test_state != TestStates.TEST_REACHABLE)
            return;

        // The device may not have a unique id and may be returned as timeout device.
        // Be careful with equalsByUniqueID which will crash on a device without an id!
        for (Device timeout_device : timeout_devices) {
            if (timeout_device != device || !timeout_device.equalsByUniqueID(device))
                continue;
            test_state = TestStates.TEST_INIT;
            if (listener != null)
                listener.testFinished(false);
            break;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean startTest(PluginService context) {
        if (device == null)
            return false;

        test_state = TestStates.TEST_REACHABLE;

        if (device.getPluginInterface() != null)
            deviceQuery = new DeviceQuery(context, this, device);
        else
            return false;
        return true;
    }

    public boolean isTesting() {
        return test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK;
    }

    public boolean isTestOK() {
        return test_state == TestStates.TEST_OK;
    }

    @Override
    public boolean updated(@NonNull DeviceCollection deviceCollection, Device updated_device, @NonNull ObserverUpdateActions action, int position) {
        if (updated_device.getUniqueDeviceID() == null || !updated_device.equalsByUniqueID(device))
            return true;

        if (!updated_device.isReachable()) {
            test_state = TestStates.TEST_INIT;
            if (listener != null)
                listener.testFinished(false);
        }

        if (test_state == TestStates.TEST_REACHABLE) {
            device.lockDevice();
            // Update stored device with received values
            device.setUniqueDeviceID(updated_device.getUniqueDeviceID());
            // do not copy the deviceName here, just the other values
            device.copyValuesFromUpdated(updated_device);
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            device.releaseDevice();
            if (deviceQuery != null) {
                deviceQuery.addDevice(deviceCollection.appData, device);
            }
            DevicePort oi = device.getFirst();
            if (oi != null)
                deviceCollection.appData.execute(oi, oi.current_value, null);

            // Timeout is 1,1s
            timeoutHandler.sendEmptyMessageDelayed(0, 1100);
        } else if (test_state == TestStates.TEST_ACCESS) {
            if (listener != null)
                listener.testFinished(true);
            device.lockDevice();
            device.copyValuesFromUpdated(updated_device);
            device.releaseDevice();
            test_state = TestStates.TEST_OK;
        }

        return true;
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public void setDevice(Device di) {
        assert di != null;
        device = di;
    }

    @Override
    public void setResultListener(onCreateDeviceResult createDeviceResult) {
        listener = createDeviceResult;
    }
}
