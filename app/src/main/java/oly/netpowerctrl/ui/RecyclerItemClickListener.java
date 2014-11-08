package oly.netpowerctrl.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
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
            public void onLongPress(MotionEvent e) {
                clickExecution(e, true);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return clickExecution(e, false);
            }

            private boolean clickExecution(MotionEvent e, boolean isLongClick) {
                if (mListenerClick != null) {
                    // Get element of the RecyclerView that was clicked
                    View childView = view.findChildViewUnder(e.getX(), e.getY());

                    if (childView == null) return false;
                    if (childView instanceof ViewGroup) {
                        ViewGroup cView = (ViewGroup) childView;
                        // We return the clicked child instead of the entire clicked element view.
                        for (int numChildren = cView.getChildCount() - 1; numChildren >= 0; --numChildren) {
                            View _child = cView.getChildAt(numChildren);
                            Rect _bounds = new Rect();
                            _child.getHitRect(_bounds);
                            if (_bounds.contains((int) (e.getX() - cView.getX()), (int) (e.getY() - cView.getY()))) {
                                if (isLongClick)
                                    _child.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                else
                                    _child.playSoundEffect(SoundEffectConstants.CLICK);
                                return mListenerClick.onItemClick(_child, view.getChildPosition(cView), isLongClick);
                            }
                        }
                    }

                    // If no child found, we return the element view.
                    if (isLongClick)
                        childView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    else
                        childView.playSoundEffect(SoundEffectConstants.CLICK);
                    return mListenerClick.onItemClick(childView, view.getChildPosition(childView), isLongClick);
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
        public boolean onItemClick(View view, int position, boolean isLongClick);
    }

    public interface OnItemFlingListener {
        public void onFling(View view, int position, float distance);
    }
}