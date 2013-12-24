package oly.netpowerctrl.utils;

import android.view.View;

public interface DeviceConfigureEvent
{
    // Request this device to be configured
    public void onConfigureDevice (View v, int position);
}