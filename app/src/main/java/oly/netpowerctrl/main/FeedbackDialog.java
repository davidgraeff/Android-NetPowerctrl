package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import oly.netpowerctrl.R;

public class FeedbackDialog extends DialogFragment {

    public FeedbackDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.netpowerctrl);
        builder.setTitle(R.string.app_name);
        builder.setItems(R.array.feedbackOptions, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) { // mail
                    @SuppressWarnings("ConstantConditions")
                    ApplicationInfo info = getActivity().getApplicationContext().getApplicationInfo();
                    PackageManager pm = getActivity().getApplicationContext().getPackageManager();
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"david.graeff@web.de"});
                    try {
                        intent.putExtra(Intent.EXTRA_SUBJECT, info.loadLabel(pm).toString() + "(" + pm.getPackageInfo(info.packageName, 0).versionName + ")" + " Contact Form | Device: " + Build.MANUFACTURER + " " + Build.DEVICE + "(" + Build.MODEL + ") API: " + Build.VERSION.SDK_INT);
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                    intent.setType("plain/html");
                    getActivity().startActivity(intent);
                } else if (which == 1) { // bugtracker
                    @SuppressWarnings("ConstantConditions")
                    Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl/issues"));
                    getActivity().startActivity(browse);
                } else if (which == 2) {
                    @SuppressWarnings("ConstantConditions")
                    Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl"));
                    getActivity().startActivity(browse);
                } else if (which == 3) {
                    @SuppressWarnings("ConstantConditions")
                    Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName()));
                    getActivity().startActivity(browse);
                }

            }
        });

        return builder.create();
    }
}
