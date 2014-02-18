package oly.netpowerctrl.main;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.OutletsCreateSceneAdapter;
import oly.netpowerctrl.shortcut.OutletsManipulator;

/**
 */
public class OutletsSceneEditFragment extends Fragment {
    private OutletsCreateSceneAdapter adapter;
    private OutletsManipulator manipulator = null;
    private int manipulator_tag;
    public static final int MANIPULATOR_TAG_INCLUDED = 0;
    public static final int MANIPULATOR_TAG_AVAILABLE = 1;

    public OutletsSceneEditFragment() {
    }

    public OutletsSceneEditFragment(Context context, int tag, OutletsManipulator manipulator) {
        // We use the constructor that is dedicated to scene editing
        this.manipulator_tag = tag;
        this.manipulator = manipulator;
        adapter = new OutletsCreateSceneAdapter(context);
    }

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_outlets_sceneedit, container, false);
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setAdapter(adapter);
        mListView.setAutomaticNumColumns(true, 350);
        mListView.setEmptyView(view.findViewById(R.id.loading));
        // We assign the empty view after a short delay time,
        // to reduce visual flicker on app start, where data
        // is loaded with a high chance within the first 500ms.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.getEmptyView().setVisibility(View.GONE);
                mListView.setEmptyView(view.findViewById(android.R.id.empty));
            }
        }, 1000);

        // If this fragment is within the scene editing activity, we need to call
        // the activity back here, to provide the gridView and adapter objects.
        manipulator.setManipulatorObjects(manipulator_tag, mListView, adapter);
        return view;
    }
}
