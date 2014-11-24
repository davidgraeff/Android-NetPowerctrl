package oly.netpowerctrl.executables;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.ui.widgets.RelativeLayoutRipple;

/**
 * An item in a devicePort adapter. Used for implementing the ViewHolder pattern.
 */
public class ExecutableViewHolder extends RecyclerView.ViewHolder implements IconDeferredLoadingThread.IconLoaded {
    final ImageView imageIcon;
    final ImageView imageEdit;
    //LinearLayout mainTextView;
    final View entry;
    final SeekBar seekBar;
    final ProgressBar progress;
    final TextView title;
    final TextView subtitle;
    final View line;
    final Drawable[] drawables = new Drawable[2];
    public int position;
    public Animation animation = null;
    boolean isNew = true;
    private boolean multiStateDrawables = false;
    private DrawableState state = DrawableState.Off;

    ExecutableViewHolder(View convertView, ExecutableAdapterItem.groupTypeEnum groupTypeEnum) {
        super(convertView);

        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.title);
        imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);

        if (groupTypeEnum == ExecutableAdapterItem.groupTypeEnum.GROUP_TYPE) {
            subtitle = null;
            imageIcon = null;
            seekBar = null;
            progress = null;
            line = convertView.findViewById(R.id.line);
            return;

        }

        line = null;
        subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
        progress = (ProgressBar) convertView.findViewById(R.id.progress);
    }

    @SuppressWarnings("SameParameterValue")
    public void loadIcon(IconDeferredLoadingThread iconCache, String uuid, LoadStoreIconData.IconState state,
                         int bitmapPosition) {
        iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                uuid, state, this, bitmapPosition));
    }

    public void reload() {
        isNew = true;
    }

    public void setBitmapOff() {
        if (state == DrawableState.Off) return;
        state = DrawableState.Off;

        if (drawables[0] == null)
            return;

        if (multiStateDrawables && animation != null) {
            final Drawable[] drawablesReverse = new Drawable[2];
            drawablesReverse[0] = drawables[1];
            drawablesReverse[1] = drawables[0];
            TransitionDrawable transitionDrawable = new TransitionDrawable(drawablesReverse);
            transitionDrawable.setCrossFadeEnabled(true);
            imageIcon.setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(500);
        } else
            imageIcon.setImageDrawable(drawables[0]);
    }

    public void setBitmapOn() {
        if (state == DrawableState.On) return;
        state = DrawableState.On;

        if (drawables[1] == null)
            return;

        if (multiStateDrawables && animation != null) {
            TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
            transitionDrawable.setCrossFadeEnabled(true);
            imageIcon.setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(500);
        } else
            imageIcon.setImageDrawable(drawables[1]);
    }

    @Override
    public void setDrawable(Drawable bitmap, int position) {
        drawables[position] = bitmap;

        if (drawables[0] != null && drawables[1] != null) {
            multiStateDrawables = true;
        }

        if (animation != null)
            return;

        if (state == DrawableState.Off && position == 0) {
            state = DrawableState.On;
            setBitmapOff();
        } else if (state == DrawableState.On && position == 1) {
            state = DrawableState.Off;
            setBitmapOn();
        }
    }

    public void animate() {
        if (progress == null || animation != null)
            return;

        setIsRecyclable(false);

        if (entry instanceof RelativeLayoutRipple) {
            ((RelativeLayoutRipple) entry).afterClickTouchEvent();
        }

        progress.setVisibility(View.VISIBLE);

        animation = new AlphaAnimation(1, 0);
        animation.setDuration(1200);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation a) {

            }

            @Override
            public void onAnimationEnd(Animation a) {
                progress.setVisibility(View.GONE);
                animation = null;
                setIsRecyclable(true);
            }

            @Override
            public void onAnimationRepeat(Animation a) {

            }
        });
        progress.startAnimation(animation);
    }

    private enum DrawableState {Off, On}
}
