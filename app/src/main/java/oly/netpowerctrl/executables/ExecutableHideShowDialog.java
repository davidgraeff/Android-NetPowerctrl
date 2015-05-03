package oly.netpowerctrl.executables;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.AdapterSourceExecutables;
import oly.netpowerctrl.executables.adapter.ExecutablesCheckableAdapter;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

;

/**
 * Presents a list of all DevicePorts/Items of a device each with a checkbox to show/hide the item.
 */
public class ExecutableHideShowDialog extends DialogFragment implements RecyclerItemClickListener.OnItemClickListener {
    RecyclerItemClickListener onItemClickListener;
    AdapterSource adapterSource;
    ExecutablesCheckableAdapter adapter;

    public ExecutableHideShowDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DataService dataService = DataService.getService();

        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_with_list, null);
        // Create list view
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // Item click listener
        onItemClickListener = new RecyclerItemClickListener(getActivity(), this, null);
        mRecyclerView.addOnItemTouchListener(onItemClickListener);
        // Adapter (Checkable list) and Adapter Source (DevicePorts of one Device)
        AdapterSourceExecutables inputOneDevicePorts = new AdapterSourceExecutables();
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.ManualCallToStart);
        adapterSource.addInput(inputOneDevicePorts);
        adapterSource.setShowHeaders(false);
        adapterSource.start(true, dataService);
        adapter = new ExecutablesCheckableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setChecked(inputOneDevicePorts.visibleExecutables());
        mRecyclerView.setAdapter(adapter);

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        return b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Set<String> shownExecutableUids = adapter.getCheckedItems();
                Collection<Executable> e = new ArrayList<>(dataService.executables.getItems().values());
                for (Executable executable : e) {
                    executable.setHidden(!shownExecutableUids.contains(executable.getUid()));
                    if (executable.hasChanged())
                        dataService.executables.put(executable);
                }
                dismiss();
            }
        }).setNegativeButton(android.R.string.cancel, null)
                .setTitle(R.string.device_shown_actions).setView(rootView).create();

    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        return false;
    }
}
