package oly.netpowerctrl.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;

/**
 * All http related stuff
 */
public class HttpThreadPool {
    private static ExecutorService pool;

    public static void startHTTP() {
        if (pool == null)
            pool = Executors.newSingleThreadExecutor();
    }

    public static void stopHTTP() {
        if (pool == null)
            return;

        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        pool = null;
    }

    public static void execute(Runnable httpRunner) {
        if (pool == null)
            startHTTP();
        pool.execute(httpRunner);
    }

    public static interface HTTPCallback<T> {
        void httpResponse(T additional, boolean callback_success, String callback_error_message);
    }

    public static <T> Runnable createHTTPRunner(final DeviceInfo device, final String getData,
                                                final String postData, final T additional,
                                                final boolean responseInMainThread, final HTTPCallback<T> callback) {
        return new Runnable() {
            @Override
            public void run() {
                URL url;
                boolean success = false;
                String result_message;
                try {
                    String cred = device.UserName + ":" + device.Password;
                    url = new URL("http://" + device.HostName + ":" + device.HttpPort + "/" + getData);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(500);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Authorization", "Basic " +
                            Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));
                    con.getOutputStream().write(postData.getBytes());
                    con.getOutputStream().flush();
                    switch (con.getResponseCode()) {
                        case 200:
                            success = true;
                            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = rd.readLine()) != null) {
                                sb.append(line);
                            }
                            result_message = sb.toString();
                            break;
                        case 401:
                            success = false;
                            result_message = NetpowerctrlApplication.instance.getString(R.string.error_device_no_access);
                            break;
                        default:
                            result_message = "code " + String.valueOf(con.getResponseCode());
                            success = false;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    result_message = e.getMessage();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                    result_message = e.getMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    result_message = e.getMessage();
                }
                final boolean callback_success = success;
                final String callback_error_message = result_message;
                if (responseInMainThread)
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.httpResponse(additional, callback_success, callback_error_message);
                        }
                    });
                else
                    callback.httpResponse(additional, callback_success, callback_error_message);
            }
        };
    }

}