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
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Automatic data synchronisation. Receive part. This server will send a welcome message like this:
 * <p/>
 * POWER_CONTROL_NEIGHBOUR_RECIPIENT
 * VERSION1
 * <p/>
 * An example receive packet is listed below. Brackets "[*]" and "{*}" are placeholders:
 * <p/>
 * POWER_CONTROL_NEIGHBOUR
 * VERSION1
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

    public static void startAutoSync() {
        if (!SharedPrefs.isNeighbourAutoSync()) {
            return;
        }
        start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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

                    while (true) {
                        Socket client;

                        try {
                            client = server.accept();
                            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                            out.println("POWER_CONTROL_NEIGHBOUR_RECIPIENT");
                            out.println("VERSION1");
                            out.flush();
                            Log.w(TAG, "neighbour data connected");
                        } catch (IOException e) {
                            System.out.println("Accept failed: 4321");

                            if (!server.isBound() || server.isClosed())
                                break;

                            continue;
                        }

                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            String line = in.readLine();
                            if (line == null || !line.equals("POWER_CONTROL_NEIGHBOUR")) continue;
                            line = in.readLine();
                            if (line == null || !line.equals("VERSION1")) {
                                ShowToast.FromOtherThread(service, "Neighbour versions not matching");
                                continue;
                            }
                            final RuntimeDataController d = NetpowerctrlApplication.getDataController();
                            Handler h = NetpowerctrlApplication.getMainThreadHandler();
                            while (true) {
                                line = in.readLine();
                                final String thisLine = line;
                                if (line == null) break;
                                switch (line) {
                                    case "SCENES":
                                        line = in.readLine();
                                        if (line == null) break;
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
                                        h.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                d.groupCollection.importData(false, thisLine);
                                            }
                                        });
                                        break;
                                    case "DEVICES":
                                        line = in.readLine();
                                        if (line == null) break;
                                        h.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                d.deviceCollection.importData(false, thisLine);
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

                                        // Get icon data (base64 encoded)
                                        line = in.readLine();
                                        byte[] icon = Base64.decode(line.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
                                        ByteArrayInputStream b = new ByteArrayInputStream(icon);
                                        Icons.saveIcon(filename, iconType, state, b);
                                        break;
                                    default:
                                        ShowToast.FromOtherThread(service, "Neighbour data: Unexpected line");
                                        Log.e(TAG, "Neighbour data: Unexpected line " + line);
                                        break;
                                }
                            }


                        } catch (IOException e) {
                            System.out.println("Read failed");
                            break;
                        }
                    }

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

    public static void start() {
        if (service != null)
            return;

        Context context = NetpowerctrlApplication.instance;
        Intent intent = new Intent(context, NeighbourDataReceiveService.class);
        context.startService(intent);
    }

    public static void stop() {
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
        service = this;
        return null;
    }
}
