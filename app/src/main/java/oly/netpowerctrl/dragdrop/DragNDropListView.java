/*
 * Copyright (C) 2010 Eric Harlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oly.netpowerctrl.dragdrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

public class DragNDropListView extends ListView implements DragDropEnabled {
    private boolean mDragMode;

    private int mStartPosition;
    private int mDragPointOffset;        //Used to adjust drag view location

    private ImageView mDragView;
    private boolean dragDropEnabled = false;

    private DropListener mDropListener;
    private DragListener mDragListener;

    public DragNDropListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isDragDropEnabled() {
        return dragDropEnabled;
    }

    @Override
    public void setDragDropEnabled(boolean mDragDropEnabled) {
        dragDropEnabled = mDragDropEnabled;
        ((DragDropEnabled) getAdapter()).setDragDropEnabled(mDragDropEnabled);
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!dragDropEnabled)
            return super.onTouchEvent(ev);

        final int action = ev.getAction();
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (action == MotionEvent.ACTION_DOWN && x < this.getWidth() / 4) {
            mDragMode = true;
        }

        if (!mDragMode)
            return super.onTouchEvent(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartPosition = pointToPosition(x, y);
                if (mStartPosition != INVALID_POSITION) {
                    int mItemPosition = mStartPosition - getFirstVisiblePosition();
                    mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                    mDragPointOffset -= ((int) ev.getRawY()) - y;
                    startDrag(mItemPosition, y);
                    drag(0, y);// replace 0 with x if desired
                }
                break;
            case MotionEvent.ACTION_MOVE:
                drag(0, y);// replace 0 with x if desired
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:
                mDragMode = false;
                stopDrag(mStartPosition - getFirstVisiblePosition());
                computeItemPositionsFromPoint(x, y);
                break;
        }
        return true;
    }

    private void computeItemPositionsFromPoint(int x, int y) {
        int mEndPosition = pointToPosition(x, y);
        if (mStartPosition != INVALID_POSITION && mEndPosition != INVALID_POSITION && mStartPosition != mEndPosition)
            if (mDropListener != null) {
                mDropListener.onDrop(mStartPosition, mEndPosition);
                getChildAt(mStartPosition).setVisibility(View.VISIBLE);
                getChildAt(mEndPosition).setVisibility(View.INVISIBLE);
                mStartPosition = mEndPosition;
            }
    }

    // move the drag view
    private void drag(int x, int y) {
        if (mDragView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();
            layoutParams.x = x;
            layoutParams.y = y - mDragPointOffset;
            WindowManager mWindowManager = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(mDragView, layoutParams);
            computeItemPositionsFromPoint(x, y);
        }
    }

    // enable the drag view for dragging
    private void startDrag(int itemIndex, int y) {
        stopDrag(itemIndex);

        View item = getChildAt(itemIndex);
        if (item == null) return;
        item.setDrawingCacheEnabled(true);
        item.setVisibility(View.INVISIBLE);
        if (mDragListener != null)
            mDragListener.onStartDrag(item);

        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);

        WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    // destroy drag view
    private void stopDrag(int itemIndex) {
        if (mDragView != null) {
            View item = getChildAt(itemIndex);
            item.setVisibility(View.VISIBLE);
            if (mDragListener != null)
                mDragListener.onStopDrag(item);
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
    }

//	private GestureDetector createFlingDetector() {
//		return new GestureDetector(getContext(), new SimpleOnGestureListener() {
//            @Override
//            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//                    float velocityY) {         	
//                if (mDragView != null) {              	
//                	int deltaX = (int)Math.abs(e1.getX()-e2.getX());
//                	int deltaY = (int)Math.abs(e1.getY() - e2.getY());
//               
//                	if (deltaX > mDragView.getWidth()/2 && deltaY < mDragView.getHeight()) {
//                		mRemoveListener.onRemove(mStartPosition);
//                	}
//                	
//                	stopDrag(mStartPosition - getFirstVisiblePosition());
//
//                    return true;
//                }
//                return false;
//            }
//        });
//	}
}
