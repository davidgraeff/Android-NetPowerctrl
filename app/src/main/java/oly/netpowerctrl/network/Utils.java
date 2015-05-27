package oly.netpowerctrl.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.Log;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

/**
 * Get network address, get broadcast address
 */
public class Utils {
    private static final String TAG = "NetworkUtils";
    private static boolean root_port_allowed = false;

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
     * Return true if the given address is presumably in the same network
     * @param otherAddress Other address
     * @return True if in current network
     */
    public static boolean addressIsInCurrentNetwork(@Nullable InetAddress otherAddress) {
        if (otherAddress == null) return false;

        try {
            for (Enumeration<NetworkInterface> networkInterfaceEnumerator = NetworkInterface.getNetworkInterfaces();
                 networkInterfaceEnumerator.hasMoreElements(); ) {

                NetworkInterface networkInterface = networkInterfaceEnumerator.nextElement();
                List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();

                for (InterfaceAddress interfaceAddress : interfaceAddressList) {
                    InetAddress inetAddress = interfaceAddress.getAddress();
                    if (inetAddress.isLoopbackAddress()) continue;

                    byte[] otherOctets = otherAddress.getAddress();
                    byte[] currentOctets = inetAddress.getAddress();
                    if (otherOctets.length != currentOctets.length) continue;


                    // Only compare host part of address

                    final int octetsHostPart = interfaceAddress.getNetworkPrefixLength() / 8;
                    boolean ok = true;
                    for (int i = 0; i < octetsHostPart; ++i) {
                        if (otherOctets[i] != currentOctets[i]) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;
                    return true;
                }
            }

        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }

        return false;
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

    private static long MacToLong(byte[] macAddress) {
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
        try {
            for (Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();
                 networkInterface.hasMoreElements(); ) {

                NetworkInterface singleInterface = networkInterface.nextElement();

                for (Enumeration<InetAddress> IpAddresses = singleInterface.getInetAddresses(); IpAddresses.hasMoreElements(); ) {
                    InetAddress inetAddress = IpAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return Utils.MacToLong(singleInterface.getHardwareAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return 0;
    }

    public static String getDateTime(Context context) {
        Calendar t = Calendar.getInstance();
        return DateFormat.getMediumDateFormat(context).format(t.getTime()).replace(".", "_") +
                " - " + DateFormat.getTimeFormat(context).format(t.getTime()).replace(":", "_");
    }

    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    public static boolean isWirelessLanConnected(Context context) {
        @SuppressWarnings("ConstantConditions")
        WifiManager cm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return cm.isWifiEnabled() && cm.getConnectionInfo() != null;
    }

    public String intToIp(int address) {
        return ((address & 0xFF) + "." +
                ((address >>>= 8) & 0xFF) + "." +
                ((address >>>= 8) & 0xFF) + "." +
                ((address >>> 8) & 0xFF));
    }

    //
//    public boolean onlyLinkLocalDevices() {
//        boolean linkLocals = true;
//        for (DeviceInfo di : deviceCollection) {
//            if (di.pluginID != DeviceInfo.DeviceType.AnelDevice)
//                continue;
//
//            try {
//                InetAddress address = InetAddress.getByName(di.hostName);
//                linkLocals &= (address.isLinkLocalAddress() || address.isSiteLocalAddress());
//            } catch (UnknownHostException e) {
//                // we couldn't resolve the device hostname to an IP address. One reason is, that
//                // the user entered a dns name instead of an IP (and the dns server is not reachable
//                // at the moment). Therefore we assume that there not only link local addresses.
//                return false;
//            }
//        }
//        return linkLocals;
//    }

}
