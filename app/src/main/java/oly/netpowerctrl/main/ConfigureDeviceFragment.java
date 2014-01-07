package oly.netpowerctrl.main;

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

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceError;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DeviceUpdate;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.anelservice.NetpowerctrlService;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.listadapter.DeviceConfigurationAdapter;

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
        args.putParcelable(DEVICE_PARAMETER, di);
        fragment.setArguments(args);
        return fragment;
    }

    private void testDevice() {
        if (test_state != TestStates.TEST_INIT && test_state != TestStates.TEST_OK)
            return;

        test_state = TestStates.TEST_REACHABLE;
        DeviceQuery dc = new DeviceQuery(getActivity(), this, device);
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
        NetpowerctrlApplication.instance.addToConfiguredDevices(device, true);
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
        builder.setPositiveButton(R.string.device_save, new DialogInterface.OnClickListener() {
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
            device = new DeviceInfo((DeviceInfo) getArguments().getParcelable(DEVICE_PARAMETER));
        }

        if (device == null) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getResources().getString(R.string.error_unknown_device),
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
            device.MacAddress = di.MacAddress;
            device.DeviceName = di.DeviceName;
            device.Outlets.clear();
            device.Outlets.addAll(di.Outlets);
            test_state = TestStates.TEST_ACCESS;
            Handler handler = new Handler();
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
            DeviceCommand ds = new DeviceCommand(device);
            DeviceSend.sendAllOutlets(getActivity(), ds, true);
        } else if (test_state == TestStates.TEST_ACCESS) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getActivity().getString(R.string.device_test_ok), Toast.LENGTH_SHORT).show();
            test_state = TestStates.TEST_OK;
        }
    }

    @Override
    public void onDeviceQueryFinished(int timeout_devices) {
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
