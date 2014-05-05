package oly.netpowerctrl.main;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsAvailableAdapter;
import oly.netpowerctrl.listadapter.DevicePortsBaseAdapter;
import oly.netpowerctrl.listadapter.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.SceneEditFragmentReady;

/**
 */
public class SceneEditFragment extends Fragment {
    private DevicePortsBaseAdapter mAdapter;
    private SceneEditFragmentReady readyObserver = null;
    private int mEditType = 0;
    public static final int TYPE_INCLUDED = 1;
    public static final int TYPE_AVAILABLE = 2;

    public SceneEditFragment() {
    }

    private void assignAdapter() {
        if (SharedPrefs.getAnimationEnabled()) {
            // Add animation to the list
            SwingBottomInAnimationAdapter animatedAdapter = new SwingBottomInAnimationAdapter(mAdapter);
            animatedAdapter.setAbsListView(mListView);
            mListView.setAbstractDynamicGridAdapter(mAdapter);
            mListView.setAdapter(animatedAdapter);
        } else {
            mListView.setAdapter(mAdapter);
        }
    }

    public void setData(Context context, int tag, SceneEditFragmentReady manipulator) {
        // We use the constructor that is dedicated to scene editing
        this.mEditType = tag;
        this.readyObserver = manipulator;
        if (tag == SceneEditFragment.TYPE_AVAILABLE) {
            mAdapter = new DevicePortsAvailableAdapter(context);
        } else if (tag == SceneEditFragment.TYPE_INCLUDED) {
            mAdapter = new DevicePortsCreateSceneAdapter(context);
        }
    }

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_outlets_sceneedit, container, false);
        assert view != null;
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setMinimumColumnWidth(350);
        mListView.setNumColumns(GridView.AUTO_FIT, container.getWidth());
        assignAdapter();
        // If this fragment is within the scene editing activity, we need to call
        // the activity back here, to provide the gridView and mAdapter objects.
        readyObserver.sceneEditFragmentReady(this);
        return view;
    }

    public DevicePortsBaseAdapter getAdapter() {
        return mAdapter;
    }

    public int getType() {
        return mEditType;
    }

    public DynamicGridView getListView() {
        return mListView;
    }

    public void checkEmpty() {
        final View view = getView();
        assert view != null;
        if (mEditType == SceneEditFragment.TYPE_AVAILABLE) {
            // We assign the empty view after a short delay time,
            // to reduce visual flicker on activity start
            Handler h = NetpowerctrlApplication.getMainThreadHandler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mListView.setEmptyView(view.findViewById(R.id.loading));

                }
            }, 10);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //noinspection ConstantConditions
                    mListView.getEmptyView().setVisibility(View.GONE);
                    mListView.setEmptyView(view.findViewById(R.id.empty_available));
                }
            }, 1000);
        } else if (mEditType == TYPE_INCLUDED)
            mListView.setEmptyView(view.findViewById(R.id.empty_included));
    }
}
