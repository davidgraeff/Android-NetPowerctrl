package oly.netpowerctrl.utils;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class GreenFlasher {

	// unused, keep as template
	@SuppressWarnings("unused")
	private static class ColorEvaluator implements TypeEvaluator<Integer> {
		public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
			int ret = 0;
			for (int i=0; i<4; i++) {
				int offs = i*8;
				ret |= (((startValue>>offs)&0xff) + (int)((((endValue>>offs)&0xff) - ((startValue>>offs)&0xff))*fraction)) << offs; 
			}
			return ret;
		}
	};
	
	private static class GreenAlphaEvaluator implements TypeEvaluator<Integer> {
		public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
			if (startValue < endValue)
				return ((int)(0xff * fraction) << 24) | 0x00ff00;
			else
				return ((0xff - (int)(0xff * fraction)) << 24) | 0x00ff00;
		}
	};
	
	static GreenAlphaEvaluator bgEvaluator = new GreenAlphaEvaluator();
	
	public static void flashBgColor(View view) { 
		// ObjectAnimator anim = ObjectAnimator.ofInt(view, "backgroundColor", 0x0000ff00, 0xff00ff00, 0x0000ff00);
		ObjectAnimator anim = ObjectAnimator.ofInt(view, "backgroundColor", 0, 1, 0);
		anim.setEvaluator(bgEvaluator);
		anim.setDuration(400);
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.start();
	}
}
