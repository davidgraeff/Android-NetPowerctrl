package oly.netpowerctrl.main;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

import oly.netpowerctrl.BuildConfig;
import oly.netpowerctrl.R;

/**
 * Application:
 * We keep track of Anel device states via the listener service.
 * Crash management
 */
public class App extends Application {
    public static App instance;
    static boolean isDebugFlag = BuildConfig.BUILD_TYPE.equals("debug");
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public App() {
        App.instance = this;
    }

    public static Handler getMainThreadHandler() {
        return instance.mainThreadHandler;
    }

    public static java.lang.String getAppString(int resId) {
        return instance.getString(resId);
    }

    public static java.lang.String getAppString(int resId, java.lang.Object... formatArgs) {
        return instance.getString(resId, formatArgs);
    }

    public static boolean isDebug() {
        return isDebugFlag;
    }

    /**
     * We do not do any loading or starting when the application is loaded.
     * This can be requested by using useService()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (!isDebugFlag) {
            ACRAConfiguration acraConfiguration = ACRA.getConfig();
            try {
                acraConfiguration.setMode(ReportingInteractionMode.TOAST);
                acraConfiguration.setFormUri("http://www.bugsense.com/api/acra?api_key=11178d09");
                acraConfiguration.setFormKey("");
                acraConfiguration.setResToastText(R.string.crash_toast_text);
                ACRA.init(this);

                //@ReportsCrashes(
//        formKey = "",
//        formUri = "https://powercontrol.cloudant.com/acra-powercontrol/_design/acra-storage/_update/report",
//        reportType = org.acra.sender.HttpSender.Type.JSON,
//        httpMethod = org.acra.sender.HttpSender.Method.PUT,
//        formUriBasicAuthLogin="waystruchatedurneintshin",
//        formUriBasicAuthPassword="bwDrmPKlGxb8vsRBLc0IRFql",
//        mode = ReportingInteractionMode.TOAST,
//        resToastText = R.string.crash_toast_text)
            } catch (ACRAConfigurationException e) {
                e.printStackTrace();
            }
            //BugSenseHandler.initAndStartSession(MyActivity.this, "11178d09");
        }
    }
}
