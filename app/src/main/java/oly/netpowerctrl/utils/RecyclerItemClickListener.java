package oly.netpowerctrl.utils;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private static final int SWIPE_MIN_DISTANCE = 160;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    GestureDetector mGestureDetector;
    private OnItemClickListener mListenerClick;
    private OnItemFlingListener mListenerFling;
    private RecyclerView view;


    public RecyclerItemClickListener(Context context, OnItemClickListener listener, OnItemFlingListener listenerFling) {
        mListenerClick = listener;
        mListenerFling = listenerFling;
        if (mListenerClick == null && mListenerFling == null)
            return;

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mListenerClick != null) {
                    // Get element of the RecyclerView that was clicked
                    ViewGroup cView = (ViewGroup) view.findChildViewUnder(e.getX(), e.getY());
                    if (cView == null) return false;

                    // We return the clicked child instead of the entire clicked element view.
                    for (int numChildren = cView.getChildCount() - 1; numChildren >= 0; --numChildren) {
                        View _child = cView.getChildAt(numChildren);
                        Rect _bounds = new Rect();
                        _child.getHitRect(_bounds);
                        if (_bounds.contains((int) (e.getX() - cView.getX()), (int) (e.getY() - cView.getY()))) {
                            return mListenerClick.onItemClick(_child, view.getChildPosition(cView));
                        }
                    }

                    // If no child found, we return the element view.
                    return mListenerClick.onItemClick(cView, view.getChildPosition(cView));
                }
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distance = e1.getX() - e2.getX();
                if (mListenerFling != null && Math.abs(distance) > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    View cView = view.findChildViewUnder(e1.getX(), e1.getY());
                    if (cView == null) return false;
                    mListenerFling.onFling(cView, view.getChildPosition(cView), distance);
                    return true;
                }
                return false;
            }
        });
    }

    public RecyclerView getRecyclerView() {
        return view;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        if (mListenerClick == null && mListenerFling == null)
            return false;

        this.view = view;
        return mGestureDetector.onTouchEvent(e);
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
    }

    public interface OnItemClickListener {
        public boolean onItemClick(View view, int position);
    }

    public interface OnItemFlingListener {
        public void onFling(View view, int position, float distance);
    }
}