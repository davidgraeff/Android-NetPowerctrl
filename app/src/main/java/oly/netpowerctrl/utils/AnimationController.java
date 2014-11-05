package oly.netpowerctrl.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by david on 08.07.14.
 */
public class AnimationController {
    public static void animateBottomViewIn(final View view) {
        if (view.getVisibility() == View.VISIBLE) // Do nothing if already visible
            return;

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                view.setVisibility(View.VISIBLE);
                view.setTranslationY(view.getHeight() * 2);
                final ObjectAnimator o = ObjectAnimator.ofFloat(view, "translationY", 0f);
                o.setDuration(500);
                o.setInterpolator(new OvershootInterpolator());
                o.start();
            }
        });
    }

    public static void animateBottomViewOut(final View view) {
        if (view.getTranslationY() != 0) // Do nothing if already out of view
            return;

        final ObjectAnimator o = ObjectAnimator.ofFloat(view, "translationY", view.getHeight() * 2);
        o.setDuration(500);
        o.setInterpolator(new LinearInterpolator());
        o.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.GONE);
            }
        });
        o.start();
    }

    public static void animateView(final View view, final boolean in, final float max) {
        float c = view.getAlpha();
        if (c >= max && in || c == 0.0f && !in) {
            return;
        }

        if (view.getAnimation() != null && !view.getAnimation().hasEnded())
            return;

        AlphaAnimation animation1 = new AlphaAnimation(c, in ? max : 0.0f);
        animation1.setInterpolator(new AccelerateInterpolator());
        animation1.setDuration(500);
        animation1.setStartOffset(0);
        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setAlpha(in ? max : 0.0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animation1);
    }

    public static Animation animateViewInOut(final View view, final boolean in, final boolean makeGone) {
        float c = view.getAlpha();
        boolean isNotVisible = view.getVisibility() != View.VISIBLE;
        if (in ? (!isNotVisible && c >= 1.0f) : (isNotVisible || c == 0.0f)) {
            return null;
        }
        view.clearAnimation();

        return animateViewInOutWithoutCheck(view, in, makeGone);
    }

    public static Animation animateViewInOutWithoutCheck(final View view, final boolean in, final boolean makeGone) {
        final AlphaAnimation animation1 = new AlphaAnimation(view.getAlpha(), in ? 1.0f : 0.0f);
        animation1.setInterpolator(new AccelerateInterpolator());
        animation1.setDuration(1000);
        animation1.setStartOffset(0);
        //animation1.setFillEnabled(true);
        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setAlpha(in ? 1.0f : 0.0f);
                if (!in && makeGone)
                    view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animation1);
        return animation1;
    }
}
