package oly.netpowerctrl.application_state;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Application:
 * We keep track of Anel device states via the listener service.
 * Crash management
 */
//@ReportsCrashes(formKey = "dGVacG0ydVHnaNHjRjVTUTEtb3FPWGc6MQ",
//        mode = ReportingInteractionMode.TOAST,
//        mailTo = "david.graeff@web.de",
//        forceCloseDialogAfterToast = false, // optional, default false
//        additionalSharedPreferences = {SharedPrefs.getInstance().PREF_WIDGET_BASENAME},
//        resToastText = R.string.crash_toast_text)

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=11178d09",
        formKey = "",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
//@ReportsCrashes(
//        formKey = "",
//        formUri = "https://powercontrol.cloudant.com/acra-powercontrol/_design/acra-storage/_update/report",
//        reportType = org.acra.sender.HttpSender.Type.JSON,
//        httpMethod = org.acra.sender.HttpSender.Method.PUT,
//        formUriBasicAuthLogin="waystruchatedurneintshin",
//        formUriBasicAuthPassword="bwDrmPKlGxb8vsRBLc0IRFql",
//        mode = ReportingInteractionMode.TOAST,
//        resToastText = R.string.crash_toast_text)
public class NetpowerctrlApplication extends Application {
    static NetpowerctrlApplication instance;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public NetpowerctrlApplication() {
        NetpowerctrlApplication.instance = this;
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

    /**
     * We do not do any loading or starting when the application is loaded.
     * This can be requested by using useService()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        //BugSenseHandler.initAndStartSession(MyActivity.this, "11178d09");
        new SharedPrefs(this); // init shared preferences singleton
    }

}
