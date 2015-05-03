package oly.netpowerctrl.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;

/**
 * All http related stuff
 */
public class HttpThreadPool {
    private static ExecutorService pool;
    private static Handler resultHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            HTTPRunner httpRunner = (HTTPRunner) msg.obj;
            httpRunner.callback.httpResponse(httpRunner.additional, msg.what > 0, httpRunner.result_message);
        }
    };

    public static void startHTTP() {
        if (pool == null)
            pool = Executors.newSingleThreadExecutor();
    }

    public static void stopHTTP() {
        if (pool == null)
            return;

        ExecutorService poolCopy = pool;
        pool = null;
        poolCopy.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!poolCopy.awaitTermination(10, TimeUnit.SECONDS)) {
                poolCopy.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!poolCopy.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            poolCopy.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public static void execute(Runnable httpRunner) {
        startHTTP();
        pool.execute(httpRunner);
    }

    public static interface HTTPCallback<T> {
        void httpResponse(T additional, boolean callback_success, String response_message);
    }

    public static class HTTPRunner<T> implements Runnable {
        private final IOConnectionHTTP deviceConnection;
        private final String getData;
        private final String postData;
        private final T additional;
        private final boolean responseInMainThread;
        private final HTTPCallback<T> callback;
        String result_message;

        public HTTPRunner(final IOConnectionHTTP deviceConnection, final String getData,
                          final String postData, final T additional,
                          final boolean responseInMainThread, final HTTPCallback<T> callback) {

            this.deviceConnection = deviceConnection;
            this.getData = getData;
            this.postData = postData;
            this.additional = additional;
            this.responseInMainThread = responseInMainThread;
            this.callback = callback;
        }

        @Override
        public void run() {
            Context context = DataService.getService();
            if (context == null)
                return;

            URL url;
            boolean success = false;
            final Credentials credentials = deviceConnection.getCredentials();
            try {
                String cred = credentials.userName + ":" + credentials.password;
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
                        result_message = context.getString(R.string.error_device_no_access);
                        break;
                    default:
                        result_message = "code " + String.valueOf(con.getResponseCode());
                }
            } catch (SocketTimeoutException e) {
                result_message = context.getString(R.string.device_timeout);
            } catch (MalformedURLException | ProtocolException e) {
                result_message = e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                result_message = e.getMessage();
            }
            final boolean callback_success = success;
            if (responseInMainThread)
                resultHandler.obtainMessage(callback_success ? 1 : 0, this).sendToTarget();
            else
                callback.httpResponse(additional, callback_success, result_message);
        }
    }

}
