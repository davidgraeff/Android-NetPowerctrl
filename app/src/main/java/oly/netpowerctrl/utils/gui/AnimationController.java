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
import java.util.HashSet;

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
    private HashSet<Long> mHighlightItemIdTopMap = new HashSet<>();
    private boolean firstAnimation = true;

    public void addHighlight(long id) {
        mHighlightItemIdTopMap.add(id);
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
                    if (mHighlightItemIdTopMap.contains(itemId)) { // highlight animation
                        child.clearAnimation();
                        child.startAnimation(highlightAnimation);
                    }
                }
                mRemoveItemIdTopMap.clear();
                mHighlightItemIdTopMap.clear();
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
