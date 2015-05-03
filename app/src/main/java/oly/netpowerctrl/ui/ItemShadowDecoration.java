package oly.netpowerctrl.ui;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Item decoration which draws drop shadow of each item views.
 */
public class ItemShadowDecoration extends RecyclerView.ItemDecoration {
    private final NinePatchDrawable mShadowDrawable;
    private final Rect mShadowPadding = new Rect();

    /**
     * Constructor.
     *
     * @param shadow 9-patch drawable used for drop shadow
     */
    public ItemShadowDecoration(NinePatchDrawable shadow) {
        mShadowDrawable = shadow;
        mShadowDrawable.getPadding(mShadowPadding);
    }

    private static boolean shouldDrawDropShadow(View child) {
        if (child.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (ViewCompat.getAlpha(child) != 1.0f) {
            return false;
        }

        Drawable background = child.getBackground();
        if (background == null) {
            return false;
        }

        if (background instanceof ColorDrawable) {
            //noinspection RedundantCast
            if (((ColorDrawable) background).getAlpha() == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int childCount = parent.getChildCount();

        if (childCount == 0) {
            return;
        }

        int savedCount = c.save(Canvas.CLIP_SAVE_FLAG);

        c.clipRect(
                parent.getLeft() + Math.max(0, parent.getPaddingLeft() - mShadowPadding.left),
                parent.getTop()/* + Math.max(0, parent.getPaddingTop() - mShadowPadding.top)*/,
                parent.getRight() - Math.max(0, parent.getPaddingRight() - mShadowPadding.right),
                parent.getBottom()/* - Math.max(0, parent.getPaddingBottom() - mShadowPadding.bottom)*/);

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);

            if (!shouldDrawDropShadow(child)) {
                continue;
            }

            final int tx = (int) (ViewCompat.getTranslationX(child) + 0.5f);
            final int ty = (int) (ViewCompat.getTranslationY(child) + 0.5f);

            final int left = child.getLeft() - mShadowPadding.left;
            final int right = child.getRight() + mShadowPadding.right;
            final int top = child.getTop() - mShadowPadding.top;
            final int bottom = child.getBottom() + mShadowPadding.bottom;

            mShadowDrawable.setBounds(left + tx, top + ty, right + tx, bottom + ty);
            mShadowDrawable.draw(c);
        }

        c.restoreToCount(savedCount);

        ViewCompat.postInvalidateOnAnimation(parent);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }
}