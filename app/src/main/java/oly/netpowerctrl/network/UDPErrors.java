package oly.netpowerctrl.network;

import android.content.Context;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

/**
 * udpSending spawns a separate thread for UDP sending and enqueues all send jobs.
 * It is used in the main service and for the neighbour discovery.
 */
public class UDPErrors {
    public static final int INQUERY_REQUEST = 0;
    public static final int INQUERY_BROADCAST_REQUEST = 1;
    public static final int NETWORK_UNREACHABLE = 2;
    public static final int NETWORK_UNKNOWN_HOSTNAME = 3;

    public static void onError(Context context, int errorID, String ip, int port, Exception e) {
        String exceptionString = (e == null || e.getMessage() == null) ? "" : e.getMessage();
        switch (errorID) {
            case INQUERY_REQUEST:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_sending_inquiry, ip) + ": " + exceptionString);
                break;
            case INQUERY_BROADCAST_REQUEST:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_sending_broadcast_inquiry, port) + ": " + exceptionString);
                break;
            case NETWORK_UNREACHABLE:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_not_in_range, ip));
                break;
            case NETWORK_UNKNOWN_HOSTNAME:
                InAppNotifications.FromOtherThread(context,
                        context.getString(R.string.error_not_in_range, ip) + ": " + exceptionString);
                break;
        }
    }
}
