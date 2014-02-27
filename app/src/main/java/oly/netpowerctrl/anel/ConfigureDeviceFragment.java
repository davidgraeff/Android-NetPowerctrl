package oly.netpowerctrl.anel;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.network.DeviceError;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.network.NetpowerctrlService;
import oly.netpowerctrl.utils.JSONHelper;

/**
 */
public class ConfigureDeviceFragment extends DialogFragment implements DeviceUpdateStateOrTimeout, DeviceUpdate, DeviceError {
    private static final String DEVICE_PARAMETER = "device";

    private enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}

    private TestStates test_state = TestStates.TEST_INIT;
    private DeviceInfo device;

    public ConfigureDeviceFragment() {
    }

    public static ConfigureDeviceFragment instantiate(Context ctx, DeviceInfo di) {
        ConfigureDeviceFragment fragment = (ConfigureDeviceFragment) Fragment.instantiate(ctx, ConfigureDeviceFragment.class.getName());
        Bundle args = new Bundle();
        args.putString(DEVICE_PARAMETER, di.toJSON());
        fragment.setArguments(args);
        return fragment;
    }

    private void testDevice() {
        if (test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK)
            return;

        test_state = TestStates.TEST_REACHABLE;
        new DeviceQuery(this, device);
    }

    private void saveDevice() {
        if (test_state != TestStates.TEST_OK) {
            //noinspection ConstantConditions
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.device_test)
                    .setMessage(R.string.device_save_without_test)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            saveAndFinish();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return;
        }

        saveAndFinish();
    }

    private void saveAndFinish() {
        NetpowerctrlService listenService = NetpowerctrlApplication.instance.getService();
        if (listenService != null) {
            listenService.restartDiscoveryThreads();
        }
        NetpowerctrlApplication.getDataController().addToConfiguredDevices(device, true);
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        NetpowerctrlService listenService = NetpowerctrlApplication.instance.getService();
        if (listenService != null) {
            listenService.removeTemporaryDevice(device);
            listenService.unregisterDeviceUpdateObserver(this);
            listenService.unregisterDeviceErrorObserver(this);
        }
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressWarnings("ConstantConditions")
        ListView view = new ListView(getActivity());
        assert device != null;
        DeviceConfigurationAdapter adapter = new DeviceConfigurationAdapter(getActivity(), device);
        view.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.configure_device);
        builder.setView(view);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.setNeutralButton(R.string.device_test, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            //noinspection ConstantConditions
            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveDevice();
                }
            });
            //noinspection ConstantConditions
            d.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //noinspection ConstantConditions
                    getFragmentManager().popBackStack();
                }
            });
            //noinspection ConstantConditions
            d.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testDevice();
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            try {
                device = DeviceInfo.fromJSON(JSONHelper.getReader(getArguments().getString(DEVICE_PARAMETER)));
            } catch (IOException e) {
            } catch (ClassNotFoundException e) {
            }
        }

        if (device == null) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getString(R.string.error_unknown_device),
                    Toast.LENGTH_LONG).show();
            //noinspection ConstantConditions
            getFragmentManager().popBackStack();
            return;
        }

        NetpowerctrlService listenService = NetpowerctrlApplication.instance.getService();
        if (listenService != null) {
            listenService.replaceTemporaryDevice(device);
            listenService.registerDeviceUpdateObserver(this);
            listenService.registerDeviceErrorObserver(this);
        }
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        if (test_state == TestStates.TEST_REACHABLE) {
            test_state = TestStates.TEST_INIT;
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getActivity().getString(R.string.error_device_not_reachable) + ": " + device.HostName + ":" + Integer.valueOf(device.SendPort).toString(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        if (!di.HostName.equals(device.HostName))
            return;

        if (test_state == TestStates.TEST_REACHABLE) {
            // Update stored device with received values
            device.UniqueDeviceID = di.UniqueDeviceID;
            device.DeviceName = di.DeviceName;
            device.copyFreshValues(di);
            // Test user+password by setting a device port.
            test_state = TestStates.TEST_ACCESS;
            // Just send the current value of the first device port as target value.
            // Should change nothing but we will get a feedback if the credentials are working.
            AnelExecutor.execute(device.DevicePorts.get(0), device.DevicePorts.get(0).current_value);
            Handler handler = new Handler();
            // Timeout is 1,1s
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (test_state == TestStates.TEST_ACCESS) {
                        test_state = TestStates.TEST_INIT;
                        //noinspection ConstantConditions
                        Toast.makeText(getActivity(),
                                getActivity().getString(R.string.error_device_no_access) + ": " + device.UserName + " " + device.Password,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }, 1100);
        } else if (test_state == TestStates.TEST_ACCESS) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getActivity().getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();
            device.reachable = true;
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {

    }

    @Override
    public void onDeviceError(String deviceName, String errMessage) {
        if (test_state == TestStates.TEST_ACCESS) {
            if (deviceName.equals(device.DeviceName)) {
                test_state = TestStates.TEST_INIT;
            }
        }
    }
}