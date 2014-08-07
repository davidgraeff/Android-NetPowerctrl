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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;

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

    public static <T> Runnable createHTTPRunner(final DeviceConnectionHTTP deviceConnection, final String getData,
                                                final String postData, final T additional,
                                                final boolean responseInMainThread, final HTTPCallback<T> callback) {
        return new Runnable() {
            @Override
            public void run() {
                URL url;
                boolean success = false;
                String result_message;
                final Device device = deviceConnection.getDevice();
                try {
                    String cred = device.UserName + ":" + device.Password;
                    url = new URL("http://" + deviceConnection.getDestinationHost()
                            + ":" + deviceConnection.getDestinationPort() + "/" + getData);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1000);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Authorization", "Basic " +
                            Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));
                    if (postData != null)
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
                            result_message = NetpowerctrlApplication.instance.getString(R.string.error_device_no_access);
                            break;
                        default:
                            result_message = "code " + String.valueOf(con.getResponseCode());
                    }
                } catch (SocketTimeoutException e) {
                    result_message = e.getMessage();
                } catch (MalformedURLException e) {
                    result_message = e.getMessage();
                } catch (ProtocolException e) {
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

    public static interface HTTPCallback<T> {
        void httpResponse(T additional, boolean callback_success, String response_message);
    }

}
