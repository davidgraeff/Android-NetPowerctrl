package oly.netpowerctrl.utils.controls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import it.gmariotti.changelibs.library.view.ChangeLogListView;
import oly.netpowerctrl.R;

/**
 * Created by david on 30.07.14.
 */
public class ChangeLogUtil {

    public static void showChangeLog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new ChangeLogDialog().show(ft, "changeLog_about");
    }

    /**
     * ChangeLogDialog
     */
    public static class ChangeLogDialog extends DialogFragment {


        public ChangeLogDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            ChangeLogListView chgList = (ChangeLogListView) layoutInflater.inflate(R.layout.changelog_fragment, null);


            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.changelog_title)
                    .setView(chgList)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();

        }
    }
}
