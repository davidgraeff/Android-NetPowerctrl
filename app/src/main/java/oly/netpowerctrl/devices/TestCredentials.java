package oly.netpowerctrl.devices;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import oly.netpowerctrl.App;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Use this class for testing device settings. Results are propagated via the onTestCredentialsResult interface.
 * The EditDeviceInterface is also implemented so this can be returned by the anel plugin for editing devices.
 */
public class TestCredentials implements DevicesObserver.onDevicesObserverFinished, onCollectionUpdated<ExecutableCollection, Executable> {
    private TestStates test_state = TestStates.TEST_INIT;
    private Credentials credentials;
    private Executable observedExecutable = null;
    private onTestCredentialsResult listener = null;

    @Override
    public void onObserverJobFinished(DevicesObserver devicesObserver) {
        final DataService dataService = DataService.getService();
        //Log.w("TestCredentials", "finished test " + credentials.reachableState().name() + " " + test_state.name());
        if (devicesObserver.isAllTimedOut()) {
            if (listener != null)
                listener.testFinished(test_state, credentials);
            test_state = TestStates.TEST_INIT;
            return;
        }

        if (test_state == TestStates.TEST_REACHABLE) {
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;

            List<Executable> l = dataService.executables.filterExecutables(credentials);
            if (l.size() > 0) {
                observedExecutable = l.get(0);
                dataService.executables.registerObserver(this);
                App.getMainThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testTimeout(dataService);
                    }
                }, 2000);
                // We first toggle the current value manually and then issue a toggle request to the device.
                // This should not change the actual device state, but will cause an update signal of the
                // received and "changed" executable (in updated(...)).
                observedExecutable.current_value = observedExecutable.getCurrentValueToggled();
                observedExecutable.execute(dataService, ExecutableAndCommand.TOGGLE, null);
            } else {
                test_state = TestStates.TEST_INIT;
                if (listener != null)
                    listener.testFinished(test_state, credentials);
            }
        }
    }

    private void testTimeout(DataService dataService) {
        if (observedExecutable == null) return;
        dataService.executables.unregisterObserver(TestCredentials.this);
        test_state = TestStates.TEST_INIT;
        if (listener != null)
            listener.testFinished(test_state, credentials);
        observedExecutable = null;
    }

    public void startTest(@NonNull DataService context, @NonNull Credentials credentials, @NonNull onTestCredentialsResult createDeviceResult) {
        if (test_state != TestStates.TEST_INIT)
            throw new RuntimeException();

        test_state = TestStates.TEST_REACHABLE;
        this.credentials = credentials;
        listener = createDeviceResult;
        context.refreshExistingDevice(credentials, this);
    }

    @Override
    public boolean updated(@NonNull ExecutableCollection executableCollection, @Nullable Executable executable, @NonNull ObserverUpdateActions action) {
        if (executable != null && executable.getUid().equals(observedExecutable.getUid())) {
            if (listener != null)
                listener.testFinished(TestStates.TEST_OK, credentials);
            test_state = TestStates.TEST_INIT;
            observedExecutable = null;
            return false;
        }
        return true;
    }
}
