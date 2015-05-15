package oly.netpowerctrl.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Fetch bug count from cloudant and from github
 */
public class GithubAndCloudant {
    private boolean isAlreadyRunningOpenAutomaticIssues = false;
    private boolean isAlreadyRunningGithub = false;

    private static IssuesDetails parseAutoIssues(InputStream con) throws IOException {
        IssuesDetails issuesDetails = new IssuesDetails();
        JsonReader reader = new JsonReader(new InputStreamReader(con));
        if (!reader.hasNext()) return null;
        reader.beginObject();
        if (!reader.hasNext()) return null;
        String name = reader.nextName();
        if (!name.equals("rows")) return null;
        reader.beginArray();
        if (!reader.hasNext()) return null;
        int count;
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                name = reader.nextName();
                switch (name) {
                    case "value":
                        count = 0;
                        reader.beginObject();
                        while (reader.hasNext()) {
                            name = reader.nextName();
                            switch (name) {
                                case "count":
                                    count = reader.nextInt();
                                    break;
                                case "latest":
                                    issuesDetails.setLatest(reader.nextLong());
                                    break;
                                case "solved":
                                    if (!reader.nextBoolean()) {
                                        ++issuesDetails.open;
                                        issuesDetails.reported_open += count;
                                    } else
                                        ++issuesDetails.closed;
                                    break;
                                default:
                                    reader.skipValue();
                                    break;
                            }
                        }
                        reader.endObject();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
        }
        reader.endArray();
        reader.endObject();
        return issuesDetails;
    }

    private static IssuesDetails parseIssues(InputStream con, final IGithubOpenIssues callback, final Handler handler) throws IOException {
        IssuesDetails issuesDetails = new IssuesDetails();
        JsonReader reader = new JsonReader(new InputStreamReader(con));
        reader.beginArray();
        while (reader.hasNext()) {
            ++issuesDetails.open;
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
        return issuesDetails;
    }

    public void getACRAIssues(final IGithubOpenIssues callback, boolean force) {
        if (isAlreadyRunningOpenAutomaticIssues)
            return;

        final String fileName = "cloudant_issues.json";
        long lastAccess = SharedPrefs.getInstance().getLastTimeOpenAutoIssuesRequested();
        if (!force && lastAccess != -1 && System.currentTimeMillis() - lastAccess < 1000 * 1800) {
            try {
                callback.gitHubOpenIssuesUpdated(parseAutoIssues(App.instance.openFileInput(fileName)), lastAccess);
                return;
            } catch (IOException ignored) {
            }
        }

        isAlreadyRunningOpenAutomaticIssues = true;

        final Handler handler = new Handler(Looper.getMainLooper());

        HttpThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                IssuesDetails issuesDetails = null;
                try {
                    String addr = App.getAppString(R.string.cloudant_bugs);
                    final URL url = new URL(addr);
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    String cred = App.getAppString(R.string.acralyzer_http_login) + ":" + App.getAppString(R.string.acralyzer_http_pwd);
                    con.setRequestProperty("Authorization", "Basic " +
                            Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));

                    switch (con.getResponseCode()) {
                        case 200:
                            InputStream inputStream = con.getInputStream();
                            FileOutputStream outputStream = App.instance.openFileOutput(fileName, 0);

                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }

                            outputStream.close();
                            inputStream.close();
                            issuesDetails = parseAutoIssues(App.instance.openFileInput(fileName));
                            SharedPrefs.getInstance().setOpenAutoIssues(System.currentTimeMillis());
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isAlreadyRunningOpenAutomaticIssues = false;
                final IssuesDetails issuesDetails_final = issuesDetails;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.gitHubOpenIssuesUpdated(issuesDetails_final, System.currentTimeMillis());
                    }
                });
            }
        });
    }

    public void getGithubIssues(final IGithubOpenIssues callback, boolean force, final String filter) {
        if (isAlreadyRunningGithub)
            return;

        final String fileName = "github_issues.json";
        final Handler handler = new Handler(Looper.getMainLooper());
        long lastAccess = SharedPrefs.getInstance().getLastTimeOpenIssuesRequested();

        if (!force && lastAccess != -1 && System.currentTimeMillis() - lastAccess < 1000 * 1800) {
            try {
                callback.gitHubOpenIssuesUpdated(parseIssues(App.instance.openFileInput(fileName), callback, handler), lastAccess);
                return;
            } catch (IOException ignored) {
            }
            return;
        }

        isAlreadyRunningGithub = true;

        HttpThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                IssuesDetails issuesDetails = null;
                String label = "";
                if (filter != null)
                    label = "&labels=" + filter;
                try {
                    String address = App.getAppString(R.string.github_issues);
                    String access = App.getAppString(R.string.github_access_token);
                    final URL url = new URL(address + "?access_token=" + access + "&state=open&per_page=100" + label);
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    switch (con.getResponseCode()) {
                        case 200:
                            InputStream inputStream = con.getInputStream();
                            FileOutputStream outputStream = App.instance.openFileOutput(fileName, 0);

                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }

                            outputStream.close();
                            inputStream.close();
                            issuesDetails = parseIssues(App.instance.openFileInput(fileName), callback, handler);
                            SharedPrefs.getInstance().setOpenIssues(System.currentTimeMillis());
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isAlreadyRunningGithub = false;
                final IssuesDetails issuesDetails_final = issuesDetails;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.gitHubOpenIssuesUpdated(issuesDetails_final, System.currentTimeMillis());
                    }
                });
            }
        });
    }

    @SuppressWarnings("unused")
    void newIssues(final String issueJson, final IGithubNewIssue callback) {

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String address = App.getAppString(R.string.github_issues);
                    String access = App.getAppString(R.string.github_access_token);
                    final URL url = new URL(address + "?access_token=" + access);
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(1500);
                    con.setRequestProperty("User-Agent", "Android.NetPowerctrl/1.0");
                    con.setRequestMethod("POST");
                    con.getOutputStream().write(issueJson.getBytes());
                    switch (con.getResponseCode()) {
                        case 200:
                            parseIssues(con.getInputStream(), callback, handler);
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
        void gitHubOpenIssuesUpdated(IssuesDetails details, long last_access);

        void gitHubIssue(int number, String title, String body);
    }

    public static class IssuesDetails {
        public int closed = 0;
        public int open = 0;
        public int reported_open = 0;
        public long latest = 0;

        void setLatest(long latest) {
            if (latest > this.latest) this.latest = latest;
        }
    }
}
