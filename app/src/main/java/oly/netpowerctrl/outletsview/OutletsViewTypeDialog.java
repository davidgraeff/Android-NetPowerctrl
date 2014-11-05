package oly.netpowerctrl.outletsview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.executables.ExecutablesSourceDemo;
import oly.netpowerctrl.executables.ExecuteAdapter;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.ui.SoftRadioGroup;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class OutletsViewTypeDialog extends DialogFragment {
    public OutletsViewTypeDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_select_view_type, null);
        RecyclerView recyclerView;

        final SoftRadioGroup softRadioGroup = new SoftRadioGroup();
        softRadioGroup.addView((RadioButton) rootView.findViewById(R.id.design1));
        softRadioGroup.addView((RadioButton) rootView.findViewById(R.id.design2));
        softRadioGroup.addView((RadioButton) rootView.findViewById(R.id.design3));

        ExecutablesSourceDemo adapterSource = new ExecutablesSourceDemo(null);
        ExecuteAdapter adapter;
        GridLayoutManager gridLayoutManager;
        int rows;

        rows = 1;
        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setLayoutRes(R.layout.list_item_icon);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list1);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                softRadioGroup.check(0);
                return true;
            }
        });

        rows = 2;
        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setLayoutRes(R.layout.grid_item_icon);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list2);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                softRadioGroup.check(1);
                return true;
            }
        });

        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setLayoutRes(R.layout.grid_item_icon_center);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list3);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                softRadioGroup.check(2);
                return true;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.outlet_choose_view_type)
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int index = softRadioGroup.getCheckedRadioButtonIndex();
                        if (index == -1)
                            return;
                        Bundle extra = OutletsViewFragment.createBundleForView(index);
                        MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName(), extra);
                    }
                });
        return builder.create();
    }
}
