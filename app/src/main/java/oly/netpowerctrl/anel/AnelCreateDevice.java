package oly.netpowerctrl.anel;

import android.os.Handler;

import java.util.List;

import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;

/**
 * Created by david on 20.08.14.
 */
public class AnelCreateDevice implements DeviceObserverResult, DeviceUpdate {
    public final Device device;
    public AnelCreateDeviceResult listener = null;
    TestStates test_state = TestStates.TEST_INIT;
    private DeviceQuery deviceQuery;

    public AnelCreateDevice(Device di) {
        if (di == null) {
            device = Device.createNewDevice(AnelUDPDeviceDiscoveryThread.anelPlugin.getPluginID());
            device.setPluginInterface(AnelUDPDeviceDiscoveryThread.anelPlugin);
            // Default values for user and password
            device.UserName = "admin";
            device.Password = "anel";
        } else
            device = di;
    }

    public boolean wakeupPlugin() {
        PluginInterface pluginInterface = device.getPluginInterface();
        if (pluginInterface != null) {
            pluginInterface.enterFullNetworkState(device);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDeviceUpdated(Device di) {
        onDeviceUpdated(di, false);
    }

    @Override
    public void onDeviceUpdated(Device updated_device, boolean willBeRemoved) {
        if (!updated_device.equalsByUniqueID(device))
            return;

        if (!updated_device.isReachable()) {
            test_state = TestStates.TEST_INIT;
            if (listener != null)
                listener.testFinished(false);
        }

        if (test_state == TestStates.TEST_REACHABLE) {
            // Update stored device with received values
            device.UniqueDeviceID = updated_device.UniqueDeviceID;
            // do not copy the deviceName here, just the other values
            device.copyValuesFromUpdated(updated_device);
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            PluginInterface pi = device.getPluginInterface();
            assert pi != null;
            if (deviceQuery != null) {
                deviceQuery.addDevice(device, false);
            }
            DevicePort oi = device.getFirst();
            if (oi != null)
                pi.execute(oi, oi.current_value, null);
            Handler handler = new Handler();
            // Timeout is 1,1s
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (test_state == TestStates.TEST_ACCESS || test_state == TestStates.TEST_INIT) {
                        test_state = TestStates.TEST_INIT;
                        if (listener != null)
                            listener.testDeviceNotReachable();
                    }
                }
            }, 1100);
        } else if (test_state == TestStates.TEST_ACCESS) {
            if (listener != null)
                listener.testFinished(true);
            device.copyValuesFromUpdated(updated_device);
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        if (test_state != TestStates.TEST_REACHABLE)
            return;

        for (Device di : timeout_devices) {
            if (!di.equalsByUniqueID(device))
                continue;
            test_state = TestStates.TEST_INIT;
            if (listener != null)
                listener.testFinished(false);
            break;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean startTest() {
        test_state = TestStates.TEST_REACHABLE;

        if (wakeupPlugin())
            deviceQuery = new DeviceQuery(this, device);
        else
            return false;
        return true;
    }

    public boolean isTesting() {
        return test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK;
    }

    enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}

    public interface AnelCreateDeviceResult {
        void testFinished(boolean success);

        void testDeviceNotReachable();
    }
}
