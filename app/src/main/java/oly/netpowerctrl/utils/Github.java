package oly.netpowerctrl.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 07.08.14.
 */
public class Github {
    private static boolean isAlreadyRunning = false;

    public static void getOpenIssues(final IGithubOpenIssues callback, boolean force) {
        if (isAlreadyRunning)
            return;

        long lastAccess = SharedPrefs.getInstance().getLastTimeOpenIssuesRequested();
        if (!force && lastAccess != -1 && System.currentTimeMillis() - lastAccess < 1000 * 1800) {
            callback.gitHubOpenIssuesUpdated(SharedPrefs.getInstance().getOpenIssues(), lastAccess);
            return;
        }

        isAlreadyRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                int open_issues = 0;
                try {
                    final URL url = new URL("https://api.github.com/repos/davidgraeff/Android-NetPowerctrl/issues?access_token=125551c2f103300dfa653efb1caffd3bdd8ed32e&state=opened&per_page=100");
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    switch (con.getResponseCode()) {
                        case 200:
                            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = rd.readLine()) != null) {
                                sb.append(line);
                            }
                            JsonReader reader = JSONHelper.getReader(sb.toString());
                            reader.beginArray();
                            while (reader.hasNext()) {
                                open_issues++;
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    reader.skipValue();
                                }
                                reader.endObject();
                            }
                            reader.endArray();
                            SharedPrefs.getInstance().setOpenIssues(open_issues, System.currentTimeMillis());
                            break;
                    }
                } catch (SocketTimeoutException e) {
                    open_issues = -2;
                    e.printStackTrace();
                } catch (IOException e) {
                    open_issues = -1;
                    e.printStackTrace();
                }
                isAlreadyRunning = false;
                final int open_issues_final = open_issues;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.gitHubOpenIssuesUpdated(open_issues_final, System.currentTimeMillis());
                    }
                });
            }
        }).start();
    }

    public interface IGithubOpenIssues {
        void gitHubOpenIssuesUpdated(int count, long last_access);
    }
}
