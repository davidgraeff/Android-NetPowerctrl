package oly.netpowerctrl.utils.gui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Created by david on 08.07.14.
 */
public class AnimationController {
    private static final int MOVE_DURATION = 150;
    private HashMap<Long, Integer> mRemoveItemIdTopMap = new HashMap<>();
    private BaseAdapter adapter;
    private AbsListView listView;

    private Animation highlightAnimation = AnimationUtils.loadAnimation(NetpowerctrlApplication.instance,
            R.anim.button_zoom);
    private Animation updateAnimation = AnimationUtils.loadAnimation(NetpowerctrlApplication.instance,
            R.anim.button_zoom);
    private Map<Long, Integer> mHighlightItemIdTopMap = new TreeMap<>();
    private Set<Long> mSmallHighlightItemIdTopMap = new TreeSet<>();
    private boolean firstAnimation = true;

    //TODO
    public void addSmallHighlight(long id) {
//        mSmallHighlightItemIdTopMap.add(id);
//        updateAnimation.reset();
    }

    public void addHighlight(long id, int view_id) {
        mHighlightItemIdTopMap.put(id, view_id);
        highlightAnimation.reset();
    }

    public void beforeRemoval(int removePosition) { // View viewToRemove
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
            View child = listView.getChildAt(i);
            int position = firstVisiblePosition + i;
//            if (child != viewToRemove) {
            if (position != removePosition) {
                long itemId = adapter.getItemId(position);
                mRemoveItemIdTopMap.put(itemId, child.getTop());
            }
        }
    }

    public void animate() {
        if (mRemoveItemIdTopMap.isEmpty() && mHighlightItemIdTopMap.isEmpty())
            return;

        firstAnimation = true;

        final ViewTreeObserver observer = listView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                int firstVisiblePosition = listView.getFirstVisiblePosition();
                for (int i = 0; i < listView.getChildCount(); ++i) {
                    final View child = listView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = adapter.getItemId(position);
                    Integer startTop = mRemoveItemIdTopMap.get(itemId);
                    int top = child.getTop();

                    if (!mRemoveItemIdTopMap.isEmpty()) { // remove animation
                        if (startTop == null) {
                            // Animate new views along with the others. The catch is that they did not
                            // exist in the start state, so we must calculate their starting position
                            // based on neighboring views.
                            int childHeight = child.getHeight() /*+ listView.getDividerHeight() */;
                            startTop = top + (i > 0 ? childHeight : -childHeight);
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            reEnableList(listView, child);
                        } else if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            reEnableList(listView, child);
                        }
                    }
                    Integer view_id = mHighlightItemIdTopMap.get(itemId);
                    if (view_id != null) {
                        View v = child.findViewById(view_id);
                        if (v != null)
                            v.startAnimation(highlightAnimation);
                    }
                    if (mSmallHighlightItemIdTopMap.contains(itemId)) {
                        child.startAnimation(updateAnimation);
                    }
                }
                mRemoveItemIdTopMap.clear();
                mHighlightItemIdTopMap.clear();
                mSmallHighlightItemIdTopMap.clear();
                return true;
            }
        });
    }

    private void reEnableList(final AbsListView listView, View child) {
        if (!firstAnimation)
            return;
        firstAnimation = false;

        // disable list for the duration of the animation
        final boolean enabledBefore = listView.isEnabled();
        listView.setEnabled(false);

        child.animate().setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //mBackgroundContainer.hideBackground();
                listView.setEnabled(enabledBefore);
            }
        });
    }

    public void setAdapter(BaseAdapter adapter) {
        this.adapter = adapter;
    }

    public void setListView(AbsListView listView) {
        this.listView = listView;
    }
}
