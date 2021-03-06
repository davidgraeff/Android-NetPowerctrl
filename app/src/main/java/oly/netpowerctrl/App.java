package oly.netpowerctrl;

import android.app.Application;
import android.os.Handler;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.main.GuiThreadHandler;
import oly.netpowerctrl.main.LifecycleHandler;

/**
 * Application:
 * We keep track of Anel device states via the listener service.
 * Crash management
 */
@ReportsCrashes()
public class App extends Application {
    static final boolean isDebugFlag = BuildConfig.BUILD_TYPE.equals("debug");
    public static App instance;
    //public static boolean useErrorReporter = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT); // Lollipop acra does not work;
    private final GuiThreadHandler mainThreadHandler = new GuiThreadHandler();
    private LifecycleHandler lifecycleHandler;

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

    public static void setErrorReportContentCrash() {
        ACRAConfiguration config = ACRA.getConfig();
        config.setCustomReportContent(new ReportField[]{ReportField.REPORT_ID, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PACKAGE_NAME, ReportField.PHONE_MODEL, ReportField.ANDROID_VERSION, ReportField.BUILD, ReportField.BRAND, ReportField.PRODUCT, ReportField.TOTAL_MEM_SIZE, ReportField.AVAILABLE_MEM_SIZE, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.USER_COMMENT, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE, ReportField.USER_EMAIL, ReportField.IS_SILENT, ReportField.DEVICE_FEATURES, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS});
        ACRA.setConfig(config);
    }

    public static void setErrorReportContentMessage() {
        ACRAConfiguration config = ACRA.getConfig();
        config.setCustomReportContent(new ReportField[]{ReportField.REPORT_ID, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PACKAGE_NAME, ReportField.PHONE_MODEL, ReportField.ANDROID_VERSION, ReportField.BUILD, ReportField.BRAND, ReportField.PRODUCT, ReportField.TOTAL_MEM_SIZE, ReportField.AVAILABLE_MEM_SIZE, ReportField.CUSTOM_DATA, ReportField.USER_COMMENT, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE, ReportField.USER_EMAIL, ReportField.IS_SILENT, ReportField.DEVICE_FEATURES, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS});
        ACRA.setConfig(config);
    }

    public static void setErrorReportContentLogFile(String filename) {
        ACRAConfiguration config = ACRA.getConfig();
        config.setApplicationLogFile(filename);
        config.setCustomReportContent(new ReportField[]{ReportField.REPORT_ID, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PACKAGE_NAME, ReportField.PHONE_MODEL, ReportField.ANDROID_VERSION, ReportField.BUILD, ReportField.BRAND, ReportField.PRODUCT, ReportField.CUSTOM_DATA, ReportField.APPLICATION_LOG, ReportField.USER_COMMENT, ReportField.USER_APP_START_DATE, ReportField.USER_EMAIL, ReportField.DEVICE_FEATURES, ReportField.SHARED_PREFERENCES});
        ACRA.setConfig(config);
    }

    /**
     * We do not do any loading or starting when the application is loaded.
     * This can be requested by using useService()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LoadStoreIconData.onCreate(this);

        ACRAConfiguration config = ACRA.getNewDefaultConfig(this);
        config.setFormUri(getString(R.string.acralyzer_http_url));
        config.setFormUriBasicAuthLogin(getString(R.string.acralyzer_http_login));
        config.setFormUriBasicAuthPassword(getString(R.string.acralyzer_http_pwd));
        config.setReportType(HttpSender.Type.JSON);
        config.setResToastText(R.string.crash_toast_text);
        config.setBuildConfigClass(BuildConfig.class);
        try {
            config.setMode(ReportingInteractionMode.TOAST);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        }
        config.setCustomReportContent(new ReportField[]{ReportField.REPORT_ID, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PACKAGE_NAME, ReportField.PHONE_MODEL, ReportField.ANDROID_VERSION, ReportField.BUILD, ReportField.BRAND, ReportField.PRODUCT, ReportField.TOTAL_MEM_SIZE, ReportField.AVAILABLE_MEM_SIZE, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.USER_COMMENT, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE, ReportField.USER_EMAIL, ReportField.IS_SILENT, ReportField.DEVICE_FEATURES, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS});
        ACRA.setConfig(config);
        ACRA.init(this);

        lifecycleHandler = new LifecycleHandler();
        registerActivityLifecycleCallbacks(lifecycleHandler);
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            LoadStoreIconData.clearIconCache();
        } else if (level >= TRIM_MEMORY_UI_HIDDEN) {
            lifecycleHandler.onBackground();
        }
        super.onTrimMemory(level);
    }
}
