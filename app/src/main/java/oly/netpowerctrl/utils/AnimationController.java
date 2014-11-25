package oly.netpowerctrl.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

/**
 * Created by david on 08.07.14.
 */
public class AnimationController {
    public static void animateBottomViewIn(final View view) {
        if (view.getVisibility() == View.VISIBLE) // Do nothing if already visible
            return;

        if (view.getHeight() == 0) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    animateBottomViewIn(view);
                }
            });
            return;
        }

        view.setVisibility(View.VISIBLE);
        view.setTranslationY(view.getHeight() * 2);
        view.animate().setDuration(800).setInterpolator(new OvershootInterpolator()).translationY(0f);
    }

    public static void animateBottomViewOut(final View view) {
        if (view.getTranslationY() != 0 || view.getVisibility() != View.VISIBLE) // Do nothing if already out of view
            return;

        view.animate().setDuration(800).translationY(view.getHeight() * 2).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.GONE);
            }
        });
    }

    public static ViewPropertyAnimator animateViewInOut(final View view, final boolean in, final boolean makeGone) {
        float c = view.getAlpha();
        boolean isNotVisible = view.getVisibility() != View.VISIBLE;
        if (in ? (!isNotVisible && c >= 1.0f) : (isNotVisible || c == 0.0f)) {
            return null;
        }
        view.clearAnimation();

        return animateViewInOutWithoutCheck(view, in, makeGone, 500);
    }

    public static ViewPropertyAnimator animateViewInOutWithoutCheck(final View view, final boolean in, final boolean makeGone, int duration) {
        if (in && (view.getVisibility() == View.INVISIBLE || view.getVisibility() == View.GONE)) {
            view.setAlpha(0);
            view.setVisibility(View.VISIBLE);
            view.setPivotY(0f);
            view.setScaleY(0.1f);
        }

        return view.animate().setDuration(duration).alpha(in ? 1.0f : 0.0f).scaleY(1f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (in)
                    return;
                if (makeGone)
                    view.setVisibility(View.GONE);
                else
                    view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    public static Animation animatePress(View view) {
        Animation a = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, view.getWidth() / 2, view.getHeight() / 2);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        a.setDuration(300);
        view.startAnimation(a);
        return a;
    }
}
