package oly.netpowerctrl.transfer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Automatic data synchronisation. Receive part. This server will send a welcome message like this:
 *
 * POWER_CONTROL_NEIGHBOUR_RECIPIENT
 * VERSION1
 * [unique_id]
 *
 * An example receive packet is listed below. Brackets "[*]" and "{*}" are placeholders:
 *
 * POWER_CONTROL_NEIGHBOUR
 * VERSION1
 * [unique_id]
 * SCENES
 * {json_with_data}
 * GROUPS
 * {json_with_data}
 * DEVICES
 * {json_with_data}
 * ICON
 * [icon_type];[icon_state];[icon filename]
 * [icon_data_base64_encoded]
 */
public class NeighbourDataReceiveService extends Service {
    private static final String TAG = "NeighbourDataReceiveService";
    private static NeighbourDataReceiveService service;
    private Thread thread;
    private static NeighbourDataSync.NeighbourDataCommunication neighbourDataCommunication;


    public static void startAutoSync() {
        if (!SharedPrefs.isNeighbourAutoSync()) {
            return;
        }
        start(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        service = this;

        if (thread == null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ServerSocket server;

                    try {
                        server = new ServerSocket(4321);
                    } catch (IOException e) {
                        System.out.println("Could not listen on port 4321");
                        return;
                    }

                    Log.w(TAG, "neighbour server ready");

                    while (!server.isClosed()) {
                        Socket client;
                        try {
                            client = server.accept();
                        } catch (IOException e) {
                            System.out.println("Accept failed: 4321");

                            if (!server.isBound() || server.isClosed())
                                break;

                            continue;
                        }

                        try {
                            clientCommunication(client);
                        } catch (IOException e) {
                            System.out.println("Read failed");
                        }
                    } // while accept client connections

                    try {
                        server.close();
                    } catch (IOException ignored) {
                    }

                    stopSelf();
                }
            });
            thread.start();
        }

        return START_STICKY;
    }

    private void clientCommunication(Socket client) throws IOException {
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        // Welcome handshake
        out.println("POWER_CONTROL_NEIGHBOUR_RECIPIENT");
        out.println("VERSION1");
        out.println("ID;" + String.valueOf(Utils.getMacAsLong()));
        out.flush();

        String line = in.readLine();
        if (line == null || !line.equals("POWER_CONTROL_NEIGHBOUR")) return;
        line = in.readLine();
        if (line == null || !line.equals("VERSION1")) {
            ShowToast.FromOtherThread(service, "Neighbour versions not matching");
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
        final long uniqueID;
        try {
            uniqueID = Long.valueOf(idParse[1]);
        } catch (NumberFormatException ignored) {
            Log.e(TAG, "Neighbour data: Unique ID missing: " + line);
            return;
        }

        boolean dataTransferred = false;

        // Data exchange
        final RuntimeDataController d = NetpowerctrlApplication.getDataController();
        Handler h = NetpowerctrlApplication.getMainThreadHandler();
        while (!client.isClosed()) {
            line = in.readLine();
            if (line == null) break;

            switch (line) {
                case "DONE":
                    client.close();
                    break;
                case "REMOVE_PAIRING":
                    if (neighbourDataCommunication != null) {
                        out.println("OK");
                        out.flush();
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                neighbourDataCommunication.pairingRemoved(uniqueID);
                            }
                        });
                    } else {
                        out.println("DENIED");
                        out.flush();
                    }
                    client.close();
                    break;
                case "SCENES":
                    line = in.readLine();
                    if (line == null) break;
                    final String thisLine = line;
                    dataTransferred = true;
                    progress(neighbourDataCommunication, h, uniqueID, "Empfange Szenen");
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            d.sceneCollection.importData(false, thisLine);
                        }
                    });
                    break;
                case "GROUPS":
                    line = in.readLine();
                    if (line == null) break;
                    final String thisLine2 = line;
                    dataTransferred = true;
                    progress(neighbourDataCommunication, h, uniqueID, "Empfange Gruppen");
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            d.groupCollection.importData(false, thisLine2);
                        }
                    });
                    break;
                case "DEVICES":
                    line = in.readLine();
                    if (line == null) break;
                    final String thisLine3 = line;
                    dataTransferred = true;
                    progress(neighbourDataCommunication, h, uniqueID, "Empfange Geräte");
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            d.deviceCollection.importData(false, thisLine3);
                        }
                    });
                    break;
                case "ICON":
                    line = in.readLine();
                    if (line == null) break;

                    // Extract icon metadata
                    String[] meta = line.split(";");
                    if (meta.length != 3) {
                        Log.e(TAG, "Neighbour data: Icon meta data read failed: " + line);
                        break;
                    }
                    Icons.IconType iconType;
                    Icons.IconState state;
                    try {
                        iconType = Icons.IconType.valueOf(meta[0]);
                        state = Icons.IconState.valueOf(meta[1]);
                    } catch (IllegalArgumentException ignored) {
                        Log.e(TAG, "Neighbour data: Icon meta data read failed: " + line);
                        break;
                    }
                    String filename = meta[2];

                    progress(neighbourDataCommunication, h, uniqueID, "Empfange icon " + filename);

                    // Get icon data (base64 encoded)
                    line = in.readLine();
                    byte[] icon = Base64.decode(line.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
                    ByteArrayInputStream b = new ByteArrayInputStream(icon);
                    Icons.saveIcon(filename, iconType, state, b);
                    dataTransferred = true;
                    break;
                default:
                    ShowToast.FromOtherThread(service, "Neighbour data: Unexpected line");
                    Log.e(TAG, "Neighbour data: Unexpected line " + line);
                    break;
            }
        } // end while
        if (dataTransferred) {
            if (neighbourDataCommunication != null) {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        neighbourDataCommunication.dataReceived(uniqueID);
                    }
                });
            }
            progress(neighbourDataCommunication, h, uniqueID, null);
            h.post(new Runnable() {
                @Override
                public void run() {
                    NetpowerctrlApplication.instance.findDevices(null);
                }
            });
        }
    }

    private static void progress(final NeighbourDataSync.NeighbourDataCommunication neighbourDataCommunication,
                                 Handler h, final long uniqueID_Receiver, final String message) {
        if (neighbourDataCommunication == null)
            return;
        h.post(new Runnable() {
            @Override
            public void run() {
                neighbourDataCommunication.dataProgress(uniqueID_Receiver, message);
            }
        });
    }

    public static void start(NeighbourDataSync.NeighbourDataCommunication neighbourDataCommunication) {
        if (neighbourDataCommunication != null)
            NeighbourDataReceiveService.neighbourDataCommunication = neighbourDataCommunication;

        if (service != null)
            return;

        Context context = NetpowerctrlApplication.instance;
        Intent intent = new Intent(context, NeighbourDataReceiveService.class);
        context.startService(intent);
    }

    public static void stop() {
        NeighbourDataReceiveService.neighbourDataCommunication = null;

        if (SharedPrefs.isNeighbourAutoSync() || service == null) {
            return;
        }

        if (service.thread != null) {
            try {
                service.thread.interrupt();
            } catch (Exception ignored) {
            }
        }
        service.stopSelf();
        service = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
