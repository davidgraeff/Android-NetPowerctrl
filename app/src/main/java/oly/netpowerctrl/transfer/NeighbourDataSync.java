package oly.netpowerctrl.transfer;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ShowToast;

/**
 * TCP based data sync
 */
public class NeighbourDataSync {
    private static final String TAG = "NeighbourDataSync";
    static AtomicBoolean isSendingData = new AtomicBoolean();

    static boolean sendData(final InetAddress ip) {
        if (isSendingData.get())
            return false;

        isSendingData.set(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket client;

                try {
                    client = new Socket(ip, 4321);

                    // Get server identification
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line = in.readLine();
                    if (line == null || !line.equals("POWER_CONTROL_NEIGHBOUR")) return;
                    line = in.readLine();
                    if (line == null || !line.equals("VERSION1")) {
                        ShowToast.FromOtherThread(NetpowerctrlApplication.instance, "Neighbour versions not matching");
                        return;
                    }

                    // Send client identification
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.println("POWER_CONTROL_NEIGHBOUR");
                    out.println("VERSION1");
                    out.flush();

                    final RuntimeDataController d = NetpowerctrlApplication.getDataController();

                    // Send data
                    out.println("SCENES");
                    out.println(d.sceneCollection.toJSON());
                    out.println("GROUPS");
                    out.println(d.groupCollection.toJSON());
                    out.println("DEVICES");
                    out.println(d.deviceCollection.toJSON());
                    // Send icons
                    Icons.IconFile[] icons = Icons.getAllIcons();
                    for (Icons.IconFile f : icons) {
                        try {
                            RandomAccessFile fR = new RandomAccessFile(f.file, "r");
                            // Get and check length
                            long longLength = fR.length();
                            int length = (int) longLength;
                            if (length != longLength)
                                throw new IOException("File size >= 2 GB");

                            byte[] icon = new byte[(int) fR.length()];
                            fR.readFully(icon);
                            out.println("ICON");
                            out.println(f.type.name() + ";" + f.state.name() + ";" + f.file.getName());
                            out.println(Base64.encodeToString(icon, Base64.NO_WRAP | Base64.NO_PADDING));
                        } catch (IOException ignored) {
                        }
                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "Neighbour not in range " + ip.getHostAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return true;
    }
}
