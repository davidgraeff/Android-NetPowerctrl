package oly.netpowerctrl.ui.widgets;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

public class EnhancedSwipeRefreshLayout extends SwipeRefreshLayout {
    private boolean enabled = true;

    public EnhancedSwipeRefreshLayout(Context context) {
        super(context);
    }

    public EnhancedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean canChildScrollUp() {
        return !enabled || super.canChildScrollUp();
    }
}