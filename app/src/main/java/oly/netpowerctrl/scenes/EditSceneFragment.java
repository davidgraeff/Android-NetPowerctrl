package oly.netpowerctrl.scenes;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.device_ports.DevicePortsBaseAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.utils.gui.AnimationController;

/**
 */
public class EditSceneFragment extends Fragment {
    private DevicePortsBaseAdapter mAdapter;
    private WeakReference<EditSceneFragmentReady> manipulatorReference;
    private ListView mListView;

    public EditSceneFragment() {
    }

    public void setReadyObserver(EditSceneFragmentReady readyObserver) {
        if (mListView != null) {
            readyObserver.sceneEditFragmentReady(this);
        } else
            manipulatorReference = new WeakReference<>(readyObserver);
    }

    /**
     * Either with remove animation or not
     *
     * @param position position
     */
    public void dismissItem(int position) {
        mAdapter.removeAt(position, true);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.create_scene_outlet_list, container, false);
        assert view != null;
        mListView = (ListView) view.findViewById(android.R.id.list);

        EditSceneFragmentReady manipulator = manipulatorReference == null ? null : manipulatorReference.get();
        if (manipulator != null) {
            manipulator.sceneEditFragmentReady(this);
            manipulatorReference = null;
        }

        return view;
    }

    public DevicePortsBaseAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(DevicePortsBaseAdapter adapter) {
        mAdapter = adapter;
        // Add animation to the list
        // Remove animation
        AnimationController animationController = new AnimationController();
        animationController.setAdapter(mAdapter);
        animationController.setListView(mListView);
        adapter.setRemoveAnimation(animationController);
        mListView.setAdapter(mAdapter);
    }

    public boolean isAvailableAdapter() {
        return mAdapter instanceof DevicePortsListAdapter;
    }

    public ListView getListView() {
        return mListView;
    }

    public void checkEmpty(boolean isTwoPaneFragment) {
        final View view = getView();
        assert view != null;
        if (isAvailableAdapter()) {
            // We assign the empty view after a short delay time,
            // to reduce visual flicker on activity start
            Handler h = NetpowerctrlApplication.getMainThreadHandler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //noinspection ConstantConditions
                    mListView.getEmptyView().setVisibility(View.GONE);
                    mListView.setEmptyView(view.findViewById(R.id.empty));
                    TextView textView = (TextView) view.findViewById(R.id.empty_text);
                    textView.setText(R.string.scene_create_helptext_available);
                    textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);
                }
            }, 200);
        } else {
            mListView.setEmptyView(view.findViewById(R.id.empty));
            TextView textView = (TextView) view.findViewById(R.id.empty_text);
            if (isTwoPaneFragment)
                textView.setText(R.string.scene_create_include_twopane);
            else
                textView.setText(R.string.scene_create_include_onepane);
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_media_ff, 0);
        }
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }
}
