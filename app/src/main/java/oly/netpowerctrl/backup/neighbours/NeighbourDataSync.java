package oly.netpowerctrl.backup.neighbours;

import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ShowToast;

/**
 * TCP based data sync
 */
public class NeighbourDataSync {
    private static final String TAG = "NeighbourDataSync";
    static AtomicBoolean isSendingData = new AtomicBoolean();

    public interface NeighbourDataCommunication {
        void pairingRemoved(long uniqueID);

        void pairingRemoveDenied(long uniqueID);

        void dataReceived(long uniqueID);

        void dataProgress(long uniqueID, String progress);
    }

    static boolean sendData(final InetAddress ip, final long uniqueID_Receiver,
                            final NeighbourDataCommunication neighbourDataCommunication,
                            final boolean removePairing) {
        if (isSendingData.get())
            return false;

        isSendingData.set(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket client = null;

                try {
                    client = new Socket(ip, 4321);

                    Log.w(TAG, "start neighbour transfer");
                    clientCommunication(client, uniqueID_Receiver, neighbourDataCommunication, removePairing);
                } catch (UnknownHostException | ConnectException e) {
                    Log.e(TAG, "Neighbour not in range " + ip.getHostAddress());
                    ShowToast.FromOtherThread(NetpowerctrlApplication.instance,
                            "Neighbour connection failed: " + ip.getHostAddress());
                    client = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (client != null)
                        client.close();
                } catch (IOException ignored) {
                }

                isSendingData.set(false);
            }
        }).start();

        return true;
    }

    private static void clientCommunication(Socket client, final long uniqueID_Receiver,
                                            final NeighbourDataCommunication neighbourDataCommunication,
                                            final boolean removePairing) throws IOException {
        // Get server identification
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        Handler h = NetpowerctrlApplication.getMainThreadHandler();

        String line = in.readLine();
        if (line == null || !line.equals("POWER_CONTROL_NEIGHBOUR_RECIPIENT")) return;
        line = in.readLine();
        if (line == null || !line.equals("VERSION1")) {
            ShowToast.FromOtherThread(NetpowerctrlApplication.instance, "Neighbour versions not matching");
            return;
        }

        // Get unique ID
        line = in.readLine();
        if (line == null) return;
        String[] idParse = line.split(";");
        if (idParse.length != 2 || !idParse[0].equals("ID")) {
            Log.e(TAG, "Neighbour data: Unique ID missing: " + line);
            return; // ID is missing
        }
        long uniqueID;
        try {
            uniqueID = Long.valueOf(idParse[1]);
        } catch (NumberFormatException ignored) {
            Log.e(TAG, "Neighbour data: Unique ID wrong format: " + line);
            return;
        }

        if (uniqueID_Receiver != uniqueID) {
            Log.e(TAG, "Neighbour data: Unique ID missmatch: " + line);
            return;
        }

        // Send client identification
        out.println("POWER_CONTROL_NEIGHBOUR");
        out.println("VERSION1");
        out.println("ID;" + String.valueOf(Utils.getMacAsLong()));
        out.flush();

        if (removePairing) {
            out.println("REMOVE_PAIRING");
            out.flush();
            line = in.readLine();
            if (line == null) return;

            if (line.equals("OK")) {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        neighbourDataCommunication.pairingRemoved(uniqueID_Receiver);
                    }
                });
            } else {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        neighbourDataCommunication.pairingRemoveDenied(uniqueID_Receiver);
                    }
                });
            }
            return;
        }

        progress(neighbourDataCommunication, h, uniqueID_Receiver, "Send data");

        final RuntimeDataController d = NetpowerctrlApplication.getDataController();

        // Send data
        out.println("SCENES");
        out.println(d.sceneCollection.toJSON());
        out.println("GROUPS");
        out.println(d.groupCollection.toJSON());
        out.println("DEVICES");
        out.println(d.deviceCollection.toJSON());
        out.flush();

        progress(neighbourDataCommunication, h, uniqueID_Receiver, "Send icons");
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
                out.flush();
            } catch (IOException ignored) {
            }
        }
        out.println("DONE");
        client.close();

        progress(neighbourDataCommunication, h, uniqueID_Receiver, null);
    }

    private static void progress(final NeighbourDataCommunication neighbourDataCommunication,
                                 Handler h, final long uniqueID_Receiver, final String message) {
        h.post(new Runnable() {
            @Override
            public void run() {
                neighbourDataCommunication.dataProgress(uniqueID_Receiver, message);
            }
        });
    }
}
