package oly.netpowerctrl.executables;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rey.material.app.Dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutablesCheckableAdapter;
import oly.netpowerctrl.executables.adapter.InputExecutables;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.SimpleListDividerDecoration;

/**
 * Presents a list of all DevicePorts/Items of a device each with a checkbox to show/hide the item.
 */
public class ExecutableHideShowDialog extends DialogFragment implements RecyclerItemClickListener.OnItemClickListener {
    RecyclerItemClickListener onItemClickListener;
    AdapterSource adapterSource;
    ExecutablesCheckableAdapter adapter;
    DataService dataService;

    public ExecutableHideShowDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
        swipeRefreshLayout.setEnabled(false);

        // Create list view
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new SimpleListDividerDecoration(ContextCompat.getDrawable(getActivity(), R.drawable.list_divider), true));
        // Item click listener
        onItemClickListener = new RecyclerItemClickListener(getActivity(), this, null);
        mRecyclerView.addOnItemTouchListener(onItemClickListener);
        // Adapter (Checkable list) and Adapter Source (DevicePorts of one Device)
        InputExecutables inputOneDevicePorts = new InputExecutables();
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.ManualCallToStart);
        adapterSource.addInput(inputOneDevicePorts);
        adapterSource.setShowHeaders(false);
        adapterSource.start(true, dataService);
        adapter = new ExecutablesCheckableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setChecked(inputOneDevicePorts.visibleExecutables());
        mRecyclerView.setAdapter(adapter);
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dataService = DataService.getService();

        Dialog dialog = new Dialog(getActivity());
        dialog.setTitle(R.string.device_shown_actions);
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<String> shownExecutableUids = adapter.getCheckedItems();
                Collection<Executable> e = new ArrayList<>(dataService.executables.getItems().values());
                for (Executable executable : e) {
                    executable.setHidden(!shownExecutableUids.contains(executable.getUid()));
                    if (executable.hasChanged())
                        dataService.executables.put(executable);
                }
                dismiss();
            }
        }).positiveAction(android.R.string.ok);
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        }).negativeAction(android.R.string.cancel);
        return dialog;
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        return false;
    }
}
