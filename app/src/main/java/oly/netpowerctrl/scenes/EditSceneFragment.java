package oly.netpowerctrl.scenes;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.BaseAdapterDecorator;
import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePortsAvailableAdapter;
import oly.netpowerctrl.devices.DevicePortsBaseAdapter;
import oly.netpowerctrl.devices.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 */
public class EditSceneFragment extends Fragment implements OnDismissCallback {
    public static final int TYPE_INCLUDED = 1;
    public static final int TYPE_AVAILABLE = 2;
    private DevicePortsBaseAdapter mAdapter;
    private AnimateDismissAdapter mAnimateDismissAdapter;
    private SwingBottomInAnimationAdapter swingBottomInAnimationAdapter;
    private int mEditType = 0;
    private WeakReference<EditSceneFragmentReady> manipulatorReference;
    private ListView mListView;

    public EditSceneFragment() {
    }

    private void assignAdapter(final EditSceneFragmentReady manipulator) {
        if (SharedPrefs.getAnimationEnabled()) {
            // Add animation to the list
            BaseAdapterDecorator animatedAdapter;
            swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);
            swingBottomInAnimationAdapter.setAbsListView(mListView);
            animatedAdapter = swingBottomInAnimationAdapter;
            // Add dismiss animation to the list
            mAnimateDismissAdapter = new AnimateDismissAdapter(animatedAdapter, this);
            mAnimateDismissAdapter.setAbsListView(mListView);
            animatedAdapter = mAnimateDismissAdapter;
            // Add swipe to dismiss animation to the list if type==included
            if (mEditType == TYPE_INCLUDED) {
                SwipeDismissAdapter swipeDismissAdapter = new SwipeDismissAdapter(animatedAdapter, new OnDismissCallback() {
                    @Override
                    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions)
                            manipulator.entryDismiss(EditSceneFragment.this, position);
                    }
                });
                swipeDismissAdapter.setAbsListView(mListView);
                animatedAdapter = swipeDismissAdapter;
            }
            mListView.setAdapter(animatedAdapter);
        } else {
            mAnimateDismissAdapter = null;
            mListView.setAdapter(mAdapter);
        }
    }

    public void setData(Context context, int tag, EditSceneFragmentReady readyObserver) {
        // We use the constructor that is dedicated to scene editing
        this.mEditType = tag;
        if (tag == EditSceneFragment.TYPE_AVAILABLE) {
            mAdapter = new DevicePortsAvailableAdapter(context);
        } else if (tag == EditSceneFragment.TYPE_INCLUDED) {
            mAdapter = new DevicePortsCreateSceneAdapter(context);
        }

        // If the view is already created, we assign the adapter immediately otherwise
        // we create a reference to the readyObserver and the adapter will be assigned
        // after view creation.
        if (mListView != null) {
            assignAdapter(readyObserver);
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
        if (mAnimateDismissAdapter != null)
            mAnimateDismissAdapter.animateDismiss(position);
        else
            mAdapter.removeAt(position, true);
    }

    /**
     * A callback that is called if animations are enabled and an item has executed a remove animation
     * and the animation finished now.
     *
     * @param listView
     * @param reverseSortedPositions
     */
    @Override
    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
        for (int position : reverseSortedPositions) {
            // If we are too fast with removing, we will have invalid indexes.
            if (position > mAdapter.getCount())
                continue;
            mAdapter.removeAt(position, false);
            mAnimateDismissAdapter.notifyDataSetChanged();
        }
        // Workaround to animate again if an item is removed and another is added later on.
        swingBottomInAnimationAdapter.setShouldAnimateFromPosition(mAdapter.getCount());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.create_scene_outlet_list, container, false);
        assert view != null;
        mListView = (ListView) view.findViewById(android.R.id.list);

        EditSceneFragmentReady manipulator = manipulatorReference == null ? null : manipulatorReference.get();
        if (manipulator != null && mAdapter != null) {
            assignAdapter(manipulator);
            manipulator.sceneEditFragmentReady(this);
            manipulatorReference = null;
        }

        return view;
    }

    public DevicePortsBaseAdapter getAdapter() {
        return mAdapter;
    }

    public int getType() {
        return mEditType;
    }

    public ListView getListView() {
        return mListView;
    }

    public void checkEmpty(boolean isTwoPaneFragment) {
        final View view = getView();
        assert view != null;
        if (mEditType == EditSceneFragment.TYPE_AVAILABLE) {
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
                    mListView.setEmptyView(view.findViewById(R.id.empty));
                    TextView textView = (TextView) view.findViewById(R.id.empty_text);
                    textView.setText(R.string.scene_create_helptext_available);
                    textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);
                }
            }, 1000);
        } else if (mEditType == TYPE_INCLUDED) {
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
        if (mAnimateDismissAdapter != null)
            mAnimateDismissAdapter.notifyDataSetChanged();
        else
            mAdapter.notifyDataSetChanged();
    }
}
