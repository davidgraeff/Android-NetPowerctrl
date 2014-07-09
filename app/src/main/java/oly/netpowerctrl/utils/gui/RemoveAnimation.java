package oly.netpowerctrl.utils.gui;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.HashMap;

/**
 * Created by david on 08.07.14.
 */
public class RemoveAnimation {
    private static final int MOVE_DURATION = 150;
    private HashMap<Long, Integer> mItemIdTopMap = new HashMap<>();
    private BaseAdapter adapter;
    private AbsListView listView;

    public void beforeRemoval(int removePosition) { // View viewToRemove
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
            View child = listView.getChildAt(i);
            int position = firstVisiblePosition + i;
//            if (child != viewToRemove) {
            if (position != removePosition) {
                long itemId = adapter.getItemId(position);
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
    }

    public void animateRemoval() {
        if (mItemIdTopMap.isEmpty())
            return;

        final ViewTreeObserver observer = listView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = listView.getFirstVisiblePosition();
                for (int i = 0; i < listView.getChildCount(); ++i) {
                    final View child = listView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = adapter.getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().withEndAction(new Runnable() {
                                    public void run() {
//                                        mBackgroundContainer.hideBackground();
                                        listView.setEnabled(true);
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() /*+ listView.getDividerHeight() */;
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().withEndAction(new Runnable() {
                                public void run() {
                                    //mBackgroundContainer.hideBackground();
                                    listView.setEnabled(true);
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                mItemIdTopMap.clear();
                return true;
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
