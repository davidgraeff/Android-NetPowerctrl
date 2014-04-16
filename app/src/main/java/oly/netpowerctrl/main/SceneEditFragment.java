package oly.netpowerctrl.main;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsAvailableAdapter;
import oly.netpowerctrl.listadapter.DevicePortsBaseAdapter;
import oly.netpowerctrl.listadapter.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.shortcut.OutletsManipulator;

/**
 */
public class SceneEditFragment extends Fragment {
    private DevicePortsBaseAdapter adapter;
    private OutletsManipulator manipulator = null;
    private int manipulator_tag;
    public static final int MANIPULATOR_TAG_INCLUDED = 0;
    public static final int MANIPULATOR_TAG_AVAILABLE = 1;

    public SceneEditFragment() {
    }

    public void setData(Context context, int tag, OutletsManipulator manipulator) {
        // We use the constructor that is dedicated to scene editing
        this.manipulator_tag = tag;
        this.manipulator = manipulator;
        if (tag == SceneEditFragment.MANIPULATOR_TAG_AVAILABLE) {
            adapter = new DevicePortsAvailableAdapter(context);
        } else if (tag == SceneEditFragment.MANIPULATOR_TAG_INCLUDED) {
            adapter = new DevicePortsCreateSceneAdapter(context);
        }
    }

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_outlets_sceneedit, container, false);
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setMinimumColumnWidth(350);
        mListView.setNumColumns(GridView.AUTO_FIT, container.getWidth());
        mListView.setAdapter(adapter);
        mListView.setEmptyView(view.findViewById(R.id.loading));
        // We assign the empty view after a short delay time,
        // to reduce visual flicker on app start, where data
        // is loaded with a high chance within the first 500ms.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.getEmptyView().setVisibility(View.GONE);
                if (manipulator_tag == MANIPULATOR_TAG_AVAILABLE)
                    mListView.setEmptyView(view.findViewById(R.id.empty_available));
                else if (manipulator_tag == MANIPULATOR_TAG_INCLUDED)
                    mListView.setEmptyView(view.findViewById(R.id.empty_included));
            }
        }, 1000);

        // If this fragment is within the scene editing activity, we need to call
        // the activity back here, to provide the gridView and adapter objects.
        manipulator.setManipulatorObjects(manipulator_tag, mListView, adapter);
        return view;
    }
}
