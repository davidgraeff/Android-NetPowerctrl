package oly.netpowerctrl.anel;

import android.support.annotation.Nullable;
import android.util.Log;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.devices.onCreateDeviceResult;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.pluginservice.DeviceObserverBase;
import oly.netpowerctrl.pluginservice.DeviceQuery;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Use this class for testing device settings. Results are propagated via the onCreateDeviceResult interface.
 * The EditDeviceInterface is also implemented so this can be returned by the anel plugin for editing devices.
 */
public class AnelEditDevice implements onDeviceObserverResult, EditDeviceInterface {
    private TestStates test_state = TestStates.TEST_INIT;
    private Device device;
    private onCreateDeviceResult listener = null;

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

    // test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK
    public TestStates getTestState() {
        return test_state;
    }

    @Override
    public void onObserverJobFinished(DeviceObserverBase deviceObserverBase) {
        Log.w("AnelEditDevice", "finished test " + device.reachableState().name() + " " + test_state.name());
        if (deviceObserverBase.isAllTimedOut()) {
            if (listener != null)
                listener.testFinished(test_state);
            test_state = TestStates.TEST_INIT;
            return;
        }

        if (test_state == TestStates.TEST_REACHABLE) {
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            new DeviceQuery(PluginService.getService(), this, device) {
                @Override
                protected void doAction(@Nullable Device device, int remainingRepeats) {
                    assert device != null;
                    DevicePort oi = device.getFirst();
                    if (oi != null)
                        ((PluginService) context).getAppData().execute(oi, oi.current_value, null);
                }
            };
        } else if (test_state == TestStates.TEST_ACCESS) {
            if (listener != null)
                listener.testFinished(TestStates.TEST_OK);
            test_state = TestStates.TEST_OK;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean startTest(PluginService context) {
        if (device == null)
            return false;

        test_state = TestStates.TEST_REACHABLE;

        if (device.getPluginInterface() != null)
            new DeviceQuery(context, this, device);
        else
            return false;
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
