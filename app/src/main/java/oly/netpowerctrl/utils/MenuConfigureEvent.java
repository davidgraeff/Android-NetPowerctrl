package oly.netpowerctrl.utils;

import android.view.View;

public interface MenuConfigureEvent {
    // Request this device to be configured
    public void onConfigure(View v, int position);
}