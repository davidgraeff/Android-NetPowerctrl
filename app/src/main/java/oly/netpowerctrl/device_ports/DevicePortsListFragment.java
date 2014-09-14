package oly.netpowerctrl.device_ports;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.AnimationController;

/**
 * Created by david on 06.09.14.
 */
public class DevicePortsListFragment extends Fragment {
    protected DevicePortsBaseAdapter mAdapter;
    protected ListView mListView;
    protected AdapterView.OnItemClickListener onItemClickListener;
    private boolean initDone = false;

    public DevicePortsListFragment() {
    }

    /**
     * Remove item at position from adapter
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
        final View view = inflater.inflate(R.layout.fragment_deviceport_list, container, false);
        assert view != null;
        mListView = (ListView) view.findViewById(android.R.id.list);
        if (onItemClickListener != null)
            mListView.setOnItemClickListener(onItemClickListener);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mAdapter != null && mListView != null && !initDone) {
            initDone = true;
            applyAdapter();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scene_included_actions, menu);
    }

    public DevicePortsBaseAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(DevicePortsBaseAdapter adapter) {
        mAdapter = adapter;
        if (mAdapter != null && mListView != null && !initDone) {
            initDone = true;
            applyAdapter();
        }
    }

    private void applyAdapter() {
        // Add animation to the list
        // Remove animation
        AnimationController animationController = new AnimationController(getActivity());
        animationController.setAdapter(mAdapter);
        animationController.setListView(mListView);
        mAdapter.setAnimationController(animationController);
        mListView.setAdapter(mAdapter);

        checkEmpty();
    }

    protected void checkEmpty() {
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        if (mListView != null)
            mListView.setOnItemClickListener(onItemClickListener);
    }
}
