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
import android.util.SparseBooleanArray;
import android.view.View;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.executables.ExecutablesListAdapter;
import oly.netpowerctrl.executables.ExecutablesSourceOneDevicePorts;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

/**
 * Presents a list of all DevicePorts/Items of a device each with a checkbox to show/hide the item.
 */
public class DeviceItemsDialog extends DialogFragment implements RecyclerItemClickListener.OnItemClickListener {
    RecyclerItemClickListener onItemClickListener;
    ExecutablesSourceOneDevicePorts executablesSourceBase;
    ExecutablesListAdapter adapter;
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

    SparseBooleanArray hiddenDevicePortArray() {
        SparseBooleanArray checked = new SparseBooleanArray();
        List<DevicePort> ports = executablesSourceBase.getDevicePortList();
        for (int i = 0; i < ports.size(); ++i) {
            if (!ports.get(i).isHidden())
                checked.put(i, true);
        }
        return checked;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null)
            device = AppData.getInstance().findDevice(bundle.getString("device"));
        if (device == null && savedInstanceState != null)
            device = AppData.getInstance().findDevice(savedInstanceState.getString("device"));

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
            executablesSourceBase = new ExecutablesSourceOneDevicePorts(null, device);
            adapter = new ExecutablesListAdapter(true, executablesSourceBase, LoadStoreIconData.iconLoadingThread, false);
            adapter.setChecked(hiddenDevicePortArray());
            mRecyclerView.setAdapter(adapter);

            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            }).setTitle(R.string.device_shown_actions).setView(rootView).create();
        } else { // not found anymore
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    List<DevicePort> list = executablesSourceBase.getDevicePortList();
                    List<Boolean> checked = adapter.getChecked();
                    for (int c = 0; c < list.size(); ++c) {
                        list.get(c).setHidden(!checked.get(c));
                    }
                    AppData.getInstance().deviceCollection.save(device);
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
