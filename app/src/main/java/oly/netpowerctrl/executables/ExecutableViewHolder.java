package oly.netpowerctrl.executables;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.Nullable;
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
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.main.App;

/**
 * An item in a devicePort adapter. Used for implementing the ViewHolder pattern.
 */
public class ExecutableViewHolder extends RecyclerView.ViewHolder implements IconDeferredLoadingThread.IconLoaded {
    @Nullable
    final ImageView imageIcon;
    //LinearLayout mainTextView;
    final View entry;
    @Nullable
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
    private Executable executable;
    private IconDeferredLoadingThread mIconLoadThread;
    private boolean showImages;

    ExecutableViewHolder(View convertView, ExecutableAdapterItem.groupTypeEnum groupTypeEnum, IconDeferredLoadingThread iconLoadThread) {
        super(convertView);

        this.mIconLoadThread = iconLoadThread;
        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.title);

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
    private void loadIcon(LoadStoreIconData.IconState state, int bitmapPosition) {
        if (imageIcon == null || !showImages) return;
        mIconLoadThread.loadIcon(new IconDeferredLoadingThread.IconItem(state, bitmapPosition, this));
    }

    public void reload() {
        isNew = true;
    }

    private void setBitmapOff() {
        if (state == DrawableState.Off || imageIcon == null) return;
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

    private void setBitmapOn() {
        if (state == DrawableState.On || imageIcon == null) return;
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

//        if (entry instanceof RelativeLayoutRipple) {
//            ((RelativeLayoutRipple) entry).afterClickTouchEvent();
//        }

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

    public void setSeekBarListener(SeekBar.OnSeekBarChangeListener onSeekBarChangeListener) {
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        }
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(@Nullable Executable executable) {
        this.executable = executable;

        if (executable == null) return;

        //            current_viewHolder.title.setTypeface(
//                    port.Hidden ? Typeface.MONOSPACE : Typeface.DEFAULT,
//                    port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);

        if (subtitle != null) {
            subtitle.setText(executable.getDescription(App.instance));
        }

        title.setText(executable.getTitle());
        title.setEnabled(executable.isEnabled());

        if (executable.isReachable())
            title.setPaintFlags(
                    title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
        else
            title.setPaintFlags(
                    title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);


        // We do this only once, if the viewHolder is new
        if (isNew) {
            switch (executable.getType()) {
                case TypeToggle: {
                    if (seekBar != null)
                        seekBar.setVisibility(View.GONE);
                    loadIcon(LoadStoreIconData.IconState.StateOff, 0);
                    loadIcon(LoadStoreIconData.IconState.StateOn, 1);
                    break;
                }
                case TypeStateless: {
                    loadIcon(LoadStoreIconData.IconState.OnlyOneState, 0);
                    if (seekBar != null)
                        seekBar.setVisibility(View.GONE);
                    setBitmapOff();
                    break;
                }
                case TypeRangedValue:
                    loadIcon(LoadStoreIconData.IconState.StateOff, 0);
                    loadIcon(LoadStoreIconData.IconState.StateOn, 1);
                    if (seekBar != null) {
                        seekBar.setVisibility(View.VISIBLE);
                        seekBar.setTag(-1);
                        seekBar.setMax(executable.getMaximumValue() - executable.getMinimumValue());
                    }
                    break;
            }

        }

        // This has to be done more often
        switch (executable.getType()) {
            case TypeStateless: {
                break;
            }
            case TypeToggle: {
                if (executable.getCurrentValue() >= executable.getMaximumValue())
                    setBitmapOn();
                else
                    setBitmapOff();
                break;
            }
            case TypeRangedValue:
                if (seekBar != null) {
                    seekBar.setTag(-1);
                    seekBar.setProgress(executable.getCurrentValue() - executable.getMinimumValue());
                    seekBar.setTag(position);
                }
                if (executable.getCurrentValue() <= executable.getMinimumValue())
                    setBitmapOff();
                else
                    setBitmapOn();
                break;
        }
    }

    public boolean isShowImages() {
        return showImages;
    }

    public void setShowImages(boolean showImages) {
        this.showImages = showImages;
    }

    private enum DrawableState {Off, On}
}
