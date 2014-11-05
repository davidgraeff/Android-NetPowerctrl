package oly.netpowerctrl.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.data.JSONHelper;

/**
 * Created by david on 07.08.14.
 */
public class Github {
    private static boolean isAlreadyRunning = false;

    public static void getOpenIssues(final IGithubOpenIssues callback, boolean force, final String filter) {
        if (isAlreadyRunning)
            return;

        long lastAccess = SharedPrefs.getInstance().getLastTimeOpenIssuesRequested();
        if (!force && lastAccess != -1 && System.currentTimeMillis() - lastAccess < 1000 * 1800) {
            callback.gitHubOpenIssuesUpdated(SharedPrefs.getInstance().getOpenIssues(), lastAccess);
            return;
        }

        isAlreadyRunning = true;

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                int open_issues = 0;
                String label = "";
                if (filter != null)
                    label = "&labels=" + filter;
                try {
                    final URL url = new URL("https://api.github.com/repos/davidgraeff/Android-NetPowerctrl/issues?access_token=5cb89af92fa58959dcb8672c688f422c0ae8fd6b&state=open&per_page=100" + label);
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    switch (con.getResponseCode()) {
                        case 200:
                            open_issues = parseIssues(con, callback, handler);
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.gitHubOpenIssuesUpdated(open_issues_final, System.currentTimeMillis());
                    }
                });
            }
        }).start();
    }

    private static int parseIssues(HttpURLConnection con, final IGithubOpenIssues callback, final Handler handler) throws IOException {
        int open_issues = 0;
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
            String body = null;
            String title = null;
            int number = -1;
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "body":
                        body = reader.nextString();
                        break;
                    case "title":
                        title = reader.nextString();
                        break;
                    case "number":
                        number = reader.nextInt();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            if (body != null && title != null && number != -1) {
                final String body_ = body;
                final String title_ = title;
                final int id_ = number;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.gitHubIssue(id_, title_, body_);
                    }
                });
            }
            reader.endObject();
        }
        reader.endArray();
        return open_issues;
    }

    public static void newIssues(final String issueJson, final IGithubNewIssue callback) {

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL("https://api.github.com/repos/davidgraeff/Android-NetPowerctrl/issues");
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    con.setRequestMethod("POST");
                    con.getOutputStream().write(issueJson.getBytes());
                    switch (con.getResponseCode()) {
                        case 200:
                            parseIssues(con, callback, handler);
                            break;
                        default:
                            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = rd.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.w("Github", sb.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.newIssueResponse(false);
                        }
                    });
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.newIssueResponse(true);
                    }
                });
            }
        }).start();
    }

    public interface IGithubNewIssue extends IGithubOpenIssues {
        void newIssueResponse(boolean success);
    }

    public interface IGithubOpenIssues {
        void gitHubOpenIssuesUpdated(int count, long last_access);

        void gitHubIssue(int number, String title, String body);
    }
}
