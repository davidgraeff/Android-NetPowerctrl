package oly.netpowerctrl.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import oly.netpowerctrl.R;

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

    public static void changeToFragment(Activity context, String fragmentClassName, String tag) {
        changeToFragment(context, Fragment.instantiate(context, fragmentClassName), tag);
    }

    public static void changeToFragment(Activity context, String fragmentClassName, final Bundle extra) {
        changeToFragment(context, Fragment.instantiate(context, fragmentClassName, extra), null);
    }

    public static void changeToFragment(Activity context, Fragment fragment, String tag) {
        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, fragment, tag);
        ft.addToBackStack(null);
        try {
            ft.commit();
        } catch (IllegalStateException exception) {
            ft.commitAllowingStateLoss();
        }
    }

}
