package oly.netpowerctrl.devices;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Iterator;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.AdapterSourceInputOneDevicePorts;
import oly.netpowerctrl.executables.ExecutablesCheckableAdapter;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

/**
 * Presents a list of all DevicePorts/Items of a device each with a checkbox to show/hide the item.
 */
public class DeviceItemsDialog extends DialogFragment implements RecyclerItemClickListener.OnItemClickListener {
    RecyclerItemClickListener onItemClickListener;
    AdapterSource adapterSource;
    ExecutablesCheckableAdapter adapter;
    private Device device;

    public DeviceItemsDialog() {
    }

    public static void show(Activity context, Device device) {
        DeviceItemsDialog newFragment = (DeviceItemsDialog) Fragment.instantiate(context, DeviceItemsDialog.class.getName());
        Bundle bundle = new Bundle();
        bundle.putString("device", device.getUniqueDeviceID());
        newFragment.setArguments(bundle);
        MainActivity.getNavigationController().changeToDialog(context, newFragment);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        final AppData appData = PluginService.getService().getAppData();
        if (bundle != null)
            device = appData.findDevice(bundle.getString("device"));
        if (device == null && savedInstanceState != null)
            device = appData.findDevice(savedInstanceState.getString("device"));

        if (device != null) {
            @SuppressLint("InflateParams")
            final View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_with_list, null);
            // Create list view
            RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            // Item click listener
            onItemClickListener = new RecyclerItemClickListener(getActivity(), this, null);
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
            // Adapter (Checkable list) and Adapter Source (DevicePorts of one Device)
            AdapterSourceInputOneDevicePorts inputOneDevicePorts = new AdapterSourceInputOneDevicePorts(device);
            adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
            adapterSource.addInput(inputOneDevicePorts);
            adapterSource.setShowHeaders(false);
            adapter = new ExecutablesCheckableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
            adapter.setChecked(inputOneDevicePorts.shownDevicePorts());
            mRecyclerView.setAdapter(adapter);

            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Set<String> shownExecutableUids = adapter.getCheckedItems();

                    device.lockDevicePorts();
                    Iterator<DevicePort> iterator = device.getDevicePortIterator();
                    while (iterator.hasNext()) {
                        DevicePort devicePort = iterator.next();
                        devicePort.setHidden(!shownExecutableUids.contains(devicePort.getUid()));
                    }
                    device.releaseDevicePorts();
                    appData.deviceCollection.save(device);
                    dismiss();
                }
            }).setTitle(R.string.device_shown_actions).setView(rootView).create();
        } else { // not found anymore
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            }).setTitle(R.string.error_device_not_found).create();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (device != null)
            outState.putString("device", device.getUniqueDeviceID());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        return false;
    }
}
