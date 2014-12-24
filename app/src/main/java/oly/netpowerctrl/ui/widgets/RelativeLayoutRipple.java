package oly.netpowerctrl.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.TouchEffectAnimator;


/**
 * User: eluleci
 * Date: 25.09.2014
 * Time: 17:23
 */
public class RelativeLayoutRipple extends RelativeLayout {

    private TouchEffectAnimator touchEffectAnimator;

    public RelativeLayoutRipple(Context context) {
        super(context);
        init();
    }

    public RelativeLayoutRipple(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        // you should set a background to view for effect to be visible. in this sample, this
        // linear layout contains a transparent background which is set inside the XML

        // giving the view to animate on
        touchEffectAnimator = new TouchEffectAnimator(this);

        // enabling ripple effect. it only performs ease effect without enabling ripple effect
        touchEffectAnimator.setHasRippleEffect(true);

        // setting the effect color
        TypedValue t = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorPrimary, t, true);
        touchEffectAnimator.setEffectColor(getResources().getColor(t.resourceId));

        // setting the duration
        touchEffectAnimator.setAnimDuration(620);

        // setting radius to clip the effect. use it if you have a rounded background
        //touchEffectAnimator.setClipRadius(20);
    }

    public void afterClickTouchEvent() {
        setWillNotDraw(false);
        touchEffectAnimator.animateTouch();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        touchEffectAnimator.onDraw(canvas);
    }
}