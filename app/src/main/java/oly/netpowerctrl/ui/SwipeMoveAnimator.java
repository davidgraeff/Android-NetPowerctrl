package oly.netpowerctrl.ui;

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;

/**
 * A {@link View.OnTouchListener} that makes the list items in a {@link AbsListView}
 * dismissable. {@link AbsListView} is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 * <p/>
 * <p>After creating the listener, the caller should also call
 * {@link AbsListView#setOnScrollListener(AbsListView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link SwipeMoveAnimator} is paused during list view
 * scrolling.</p>
 * <p/>
 * <p>Example usage:</p>
 * <p/>
 * <pre>
 * SwipeDismissListViewTouchListener touchListener =
 *         new SwipeDismissListViewTouchListener(
 *                 listView,
 *                 new SwipeDismissListViewTouchListener.OnDismissCallback() {
 *                     public void onSwipeComplete(AbsListView listView, int[] reverseSortedPositions) {
 *                         for (int position : reverseSortedPositions) {
 *                             adapter.remove(adapter.getItem(position));
 *                         }
 *                         adapter.notifyDataSetChanged();
 *                     }
 *                 });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 * <p/>
 * <p>This class Requires API level 12 or later due to use of {@link
 * ViewPropertyAnimator}.</p>
 * <p/>
 */
public class SwipeMoveAnimator implements View.OnTouchListener {
    private static final int DENSITY_INDEPENDENT_THRESHOLD = 200;
    // Cached ViewConfiguration and system-wide constant values
    private final int mSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    private final long mAnimationTime;
    // Fixed properties
    private final DismissCallbacks mCallbacks;
    private final int SWIPE_THRESHOLD_VELOCITY;
    // Transient properties
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private boolean mPaused;
    private boolean mLastDirectionRight = false;
    private boolean mDismissAllowed = false;
    private View swipeView = null;
    private float offsetX;

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param context   The list which is dismissable.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     *                  dismiss one or more list items.
     */
    public SwipeMoveAnimator(Context context, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        mCallbacks = callbacks;
        Resources r = context.getResources();
        float density = r.getDisplayMetrics().density;
        SWIPE_THRESHOLD_VELOCITY = (int) (DENSITY_INDEPENDENT_THRESHOLD * density);
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * Returns an {@link AbsListView.OnScrollListener} to be added to the {@link
     * AbsListView} using {@link AbsListView#setOnScrollListener(AbsListView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this {@link SwipeMoveAnimator} is
     * paused during list view scrolling.</p>
     *
     * @see SwipeMoveAnimator
     */
    public RecyclerView.OnScrollListener makeScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setEnabled(newState == RecyclerView.SCROLL_STATE_IDLE);
            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        final View mView = swipeView == null ? view : swipeView;
        int mViewWidth = mView.getWidth();

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                mDownX = motionEvent.getRawX();
                mDownY = motionEvent.getRawY();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mSwiping) {
                    // cancel
                    mView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean swipeComplete = false;
                boolean swipeRight = false;
                if (mDismissAllowed) {
                    if (Math.abs(deltaX) > mViewWidth / 3 && mSwiping) {
                        swipeComplete = true;
                        swipeRight = deltaX > 0;
                    } else if (SWIPE_THRESHOLD_VELOCITY > absVelocityX   //mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                            && absVelocityY < absVelocityX && mSwiping) {
                        // swipeComplete only if flinging in the same direction as dragging
                        swipeComplete = (velocityX < 0) == (deltaX < 0);
                        swipeRight = mVelocityTracker.getXVelocity() > 0;
                    }
                }
                if (swipeComplete) {
                    final boolean fromLeftToRight = swipeRight;
                    mView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mView.setAlpha(1);
                            mView.setTranslationX(0);
                            mCallbacks.onSwipeComplete(fromLeftToRight, true);
                        }
                    });
                } else {
                    // cancel
                    mView.animate()
                            .translationX(offsetX)
                            .alpha(0)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                    mCallbacks.onSwipeComplete(swipeRight, false);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                boolean dismissRight = deltaX > 0;
                if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    if (!mSwiping || mLastDirectionRight != dismissRight) {
                        mLastDirectionRight = dismissRight;
                        mDismissAllowed = mCallbacks.onSwipeStarted(dismissRight);
                    }
                    mSwiping = true;
                    //((ViewParent)mView).requestDisallowInterceptTouchEvent(true);

                    // Cancel AbsListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping) {
                    float alpha = 1f - Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaX) / mViewWidth));

                    mCallbacks.onSwipeProgress(dismissRight, alpha);

                    if (dismissRight)
                        mView.setTranslationX(offsetX + deltaX + mSlop);
                    else
                        mView.setTranslationX(offsetX + deltaX - mSlop);
                    //Log.w("SW", String.valueOf(alpha)+ " "+ String.valueOf(offsetX + deltaX));
                    if (mDismissAllowed)
                        mView.setAlpha(alpha);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void setSwipeView(View view) {
        swipeView = view;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * The callback interface used by {@link SwipeMoveAnimator} to inform its client
     * about a successful dismissal.
     */
    public interface DismissCallbacks {
        /**
         * Called when the dismiss action has been finished/aborted by the user.
         *
         * @param fromLeftToRight Indicate if the view has been dismissed to the right.
         * @param finished        If true then the dismissal has been finished otherwise it has been aborted.
         */
        void onSwipeComplete(boolean fromLeftToRight, boolean finished);

        /**
         * Called when a dismiss action starts the first time. May be called another time,
         * if the user changes the direction of dismissal.
         *
         * @param fromLeftToRight Indicate if the view has been dismissed to the right.
         * @return Return false if the dismissal action should be marked as not possible.
         */
        boolean onSwipeStarted(boolean fromLeftToRight);

        /**
         * Indicates the offset of the view during dismissal action. Value between 0f and 1f.
         */
        void onSwipeProgress(boolean fromLeftToRight, float offset);
    }
}