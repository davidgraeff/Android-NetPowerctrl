package oly.netpowerctrl.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 31.01.15.
 */
public class FragmentUtils {
    public static void changeToDialog(Activity context, String fragmentClassName) {
        FragmentManager fragmentManager = context.getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ((DialogFragment) Fragment.instantiate(context, fragmentClassName)).show(ft, "dialog");
    }

    public static void changeToDialog(Activity context, DialogFragment fragment) {
        FragmentManager fragmentManager = context.getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        fragment.show(ft, "dialog");
    }


    public static void changeToFragment(Activity context, String fragmentClassName) {
        changeToFragment(context, Fragment.instantiate(context, fragmentClassName), null);
    }

    public static void changeToFragment(Activity context, String fragmentClassName, String tag, Bundle extra) {
        Fragment fragment = context.getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            context.getFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fragment = Fragment.instantiate(context, fragmentClassName, extra);
        changeToFragment(context, fragment, tag);
    }

    public static void changeToFragment(Activity context, Fragment fragment, String tag) {
        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, fragment, tag);
        ft.addToBackStack(tag);
        try {
            ft.commit();
        } catch (IllegalStateException exception) {
            ft.commitAllowingStateLoss();
        }
    }

    public static void unloadFragment(Activity context, String tag) {
        Fragment groupFragment = context.getFragmentManager().findFragmentByTag(tag);
        if (groupFragment != null) {
            FragmentTransaction ft = context.getFragmentManager().beginTransaction();
            ft.remove(groupFragment).commitAllowingStateLoss();
        }
    }

    public static void loadFragment(Activity context, String fragmentClassName, @IdRes int id, String tag) {
        loadFragment(context, Fragment.instantiate(context, fragmentClassName), id, tag);
    }

    public static void loadFragment(Activity context, Fragment fragment, @IdRes int id, String tag) {
        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
        ft.replace(id, fragment, tag);
        try {
            ft.commit();
        } catch (IllegalStateException exception) {
            ft.commitAllowingStateLoss();
        }
    }

    public static void makeActivityDialog(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.alpha = 1f;    // lower than one makes it more transparent
        params.dimAmount = 0.2f;  // set it higher if you want to dim behind the window
        activity.getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            activity.getWindow().setLayout((int) (width * .8), (int) (height * .6));
        } else {
            activity.getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }
    }

    public static void applyActivityFlags(AppCompatActivity activity) {
        //Remove title bar
        activity.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        if (SharedPrefs.getInstance().isFullscreen()) {
            //Remove notification bar
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // on android5+ color of system bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int c = SharedPrefs.getInstance().isDarkTheme() ?
                    activity.getResources().getColor(R.color.colorSecondaryDark) :
                    activity.getResources().getColor(R.color.colorSecondaryLight);
            activity.getWindow().setStatusBarColor(c);
            activity.getWindow().setNavigationBarColor(c);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = activity.getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (SharedPrefs.getInstance().isDarkTheme()) {
            activity.setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            activity.setTheme(R.style.Theme_CustomLightTheme);
        }
    }
}
