package oly.netpowerctrl.executables;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.rey.material.app.Dialog;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutablesAdapter;
import oly.netpowerctrl.executables.adapter.InputDemo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.SoftRadioGroup;

/**
 * Set view mode (list/grid/etc) of the executables.
 */
public class ExecutablesViewModeDialog extends DialogFragment {
    final SoftRadioGroup radioGroupDesign = new SoftRadioGroup();

    public ExecutablesViewModeDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_select_view_type, null);

        RecyclerView recyclerView;

        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design1));
        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design2));
        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design3));
        radioGroupDesign.check(SharedPrefs.getInstance().getOutletsViewType());

        ExecutablesAdapter adapter;
        AdapterSource adapterSource;
        GridLayoutManager gridLayoutManager;
        int rows;

        rows = 1;
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new InputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.list_item_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list1);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector1 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(0);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector1.onTouchEvent(motionEvent);
                return true;
            }
        });

        rows = 2;
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new InputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.grid_item_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list2);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector2 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(1);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector2.onTouchEvent(motionEvent);
                return true;
            }
        });

        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new InputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.grid_item_compact_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list3);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector3 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(2);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector3.onTouchEvent(motionEvent);
                return true;
            }
        });
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setTitle(R.string.view_mode);
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index;
                index = radioGroupDesign.getCheckedRadioButtonIndex();
                if (index != -1) {
                    SharedPrefs.getInstance().setOutletsViewType(index);
                }
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
}
