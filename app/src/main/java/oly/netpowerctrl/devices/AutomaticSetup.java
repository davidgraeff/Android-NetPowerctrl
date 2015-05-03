package oly.netpowerctrl.devices;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class AutomaticSetup implements onTestCredentialsResult, onServiceReady, onCollectionUpdated<CredentialsCollection, Credentials> {
    private Button button;
    private View welcome_text;
    private TextView find_device_status;
    private List<Credentials> credentialsList;
    private TestCredentials testCredentials = new TestCredentials();
    private int current = 0;
    private boolean isRunning = false;
    private DataService dataService = null;
    private int statistic_configured = 0;
    private int statistic_failures = 0;
    private Handler takeNextHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    takeNext();
                    break;
                case 1:
                    finishAnimate();
            }
        }
    };
    //private View item;

    public AutomaticSetup(Button button, TextView find_device_status, View welcome_text) {
        this.button = button;
        this.find_device_status = find_device_status;
        this.welcome_text = welcome_text;

        button.findViewById(R.id.automatic_setup_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        DataService.observersServiceReady.register(this);
    }

    public void start() {
        if (dataService == null) return;
        dataService.credentials.registerObserver(this);

        int n = dataService.credentials.size();
        if (n == 0) {
            dataService.showNotificationForNextRefresh(true);
            dataService.refreshDevices();
            return;
        }

        find_device_status.setText("Richte Geräte ein...");

        credentialsList = new ArrayList<>(dataService.credentials.getItems().values());
        if (credentialsList.size() == 0) return;

        isRunning = true;
        button.setEnabled(false);
        takeNext();
    }

    private void finishAnimate() {
        AnimationController.animateViewInOutWithoutCheck(button, false, true, 500);
        AnimationController.animateViewInOutWithoutCheck(find_device_status, false, true, 500);
        AnimationController.animateViewInOutWithoutCheck(welcome_text, false, true, 500);
        finish();
    }

    public void finish() {
        if (dataService != null)
            dataService.credentials.unregisterObserver(this);
        dataService = null;
        button = null;
        welcome_text = null;
        find_device_status = null;
        credentialsList = null;
    }

    void takeNext() {
        //LayoutInflater l = LayoutInflater.from(find_device_status.getContext());
        //item = l.inflate(R.layout.list_item_device, button);

        if (current >= credentialsList.size()) {
            isRunning = false;
            find_device_status.setText(App.getAppString(R.string.automatic_finished_result, statistic_configured, statistic_failures));
            takeNextHandler.sendEmptyMessageDelayed(1, dataService.credentials.size() > 0 ? 1500 : 3000);
            return;
        }

        Credentials credentials = credentialsList.get(current);

        //item.findViewById(R.id.device_connection_reachable).setVisibility(View.GONE);

        ++current;
        //((TextView) item.findViewById(R.id.title)).setText(device.getDeviceName());

        // Skip already configured devices
        if (credentials.isConfigured()) {
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        AbstractBasePlugin plugin = credentials.getPlugin();
        if (plugin == null) {
            //((TextView) item.findViewById(R.id.subtitle)).append(App.getAppString(R.string.error_plugin_not_installed));
            takeNextHandler.sendEmptyMessage(0);
            return;
        }

        //((TextView) item.findViewById(R.id.subtitle)).append(App.getAppString(R.string.device_connection_testing));

        testCredentials.startTest(dataService, credentials, this);
    }

    @Override
    public void testFinished(TestStates state, final Credentials credentialsToAdd) {
        if (state == TestStates.TEST_OK) {
            //((TextView) item.findViewById(R.id.subtitle)).append(App.getAppString(android.R.string.ok));
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    // Add to group with name DeviceName
                    Group group = dataService.groups.put(null, credentialsToAdd.getDeviceName());
                    List<Executable> executableList = dataService.executables.filterExecutables(credentialsToAdd);
                    for (Executable e : executableList) {
                        e.addToGroup(group.getUid());
                        dataService.executables.put(e);
                    }
                    // Add device to configured devices
                    dataService.addToConfiguredDevices(credentialsToAdd);
                }
            });
//        } else if (state == EditDeviceInterface.TestStates.TEST_ACCESS) {
//        } else if (state == EditDeviceInterface.TestStates.TEST_REACHABLE) {
        }
        takeNextHandler.sendEmptyMessage(0);
    }

    @Override
    public boolean onServiceReady(DataService service) {
        this.dataService = service;
        dataService.credentials.registerObserver(this);
        updateStatusTextWithUnconfiguredNumber();
        return false;
    }

    @Override
    public void onServiceFinished(DataService service) {
        service.credentials.unregisterObserver(this);
        dataService = null;
    }

    @Override
    public boolean updated(@NonNull CredentialsCollection o, @Nullable Credentials o2, @NonNull ObserverUpdateActions action) {
        if (!isRunning) updateStatusTextWithUnconfiguredNumber();
        return true;
    }

    private void updateStatusTextWithUnconfiguredNumber() {
        int n = dataService != null ? dataService.credentials.countNotConfigured() : 0;
        if (n == 0) {
            find_device_status.setText(App.getAppString(R.string.automatic_not_found_unconfigured_devices));
            button.setText(App.getAppString(R.string.automatic_refresh_devices));
        } else {
            find_device_status.setText(App.getAppString(R.string.automatic_found_unconfigured_devices, n));
            button.setText(App.getAppString(R.string.automatic_configuration));
        }
    }
}
