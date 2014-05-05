package oly.netpowerctrl.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Get network address, get broadcast address
 */
public class Utils {
    public static final String TAG = "NetworkUtils";

    /**
     * Return broadcast address of given IP or null if no broadcast address exists (IPv6).
     *
     * @return Broadcast address
     */
    public static InetAddress getBroadcast(InetAddress currentIPAddress) {
        if (currentIPAddress == null)
            return null;

        try {
            NetworkInterface temp = NetworkInterface.getByInetAddress(currentIPAddress);
            List<InterfaceAddress> addresses = temp.getInterfaceAddresses();

            for (InterfaceAddress interfaceAddress : addresses) {
                InetAddress inetAddress = interfaceAddress.getBroadcast();
                if (inetAddress != null) {
                    return inetAddress;
                }
            }
        } catch (SocketException ignored) {
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * Return first valid ip address
     *
     * @return First valid ip address
     */
    public static InetAddress getIpv4Address() {
        try {

            InetAddress inetAddress = null;
            InetAddress myAddress = null;

            for (Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();
                 networkInterface.hasMoreElements(); ) {

                NetworkInterface singleInterface = networkInterface.nextElement();

                for (Enumeration<InetAddress> IpAddresses = singleInterface.getInetAddresses(); IpAddresses.hasMoreElements(); ) {
                    inetAddress = IpAddresses.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address &&
                            (singleInterface.getDisplayName().contains("wlan") ||
                                    singleInterface.getDisplayName().contains("eth"))) {

                        myAddress = inetAddress;
                        return myAddress;
                    }
                }
            }

        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static long MacToLong(byte[] macAddress) {
        if (macAddress == null || macAddress.length != 6)
            return 0;

        long mac = 0;
        for (int i = 0; i < 6; i++) {
            long t = (macAddress[i] & 0xffL) << ((5 - i) * 8);
            mac |= t;
        }
        return mac;
    }

    public static long getMacAsLong() {
        NetworkInterface ni = null;
        try {
            ni = NetworkInterface.getByInetAddress(Utils.getIpv4Address());
            ni.getHardwareAddress();
            return Utils.MacToLong(ni.getHardwareAddress());
        } catch (SocketException ignored) {
            return 0;
        }
    }

    public static int getVersionCode() {
        Context c = NetpowerctrlApplication.instance;
        try {
            //noinspection ConstantConditions
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getDateTime() {
        Context context = NetpowerctrlApplication.instance;
        Calendar t = Calendar.getInstance();
        return DateFormat.getMediumDateFormat(context).format(t.getTime()).replace(".", "_") +
                " - " + DateFormat.getTimeFormat(context).format(t.getTime()).replace(":", "_");
    }
}
