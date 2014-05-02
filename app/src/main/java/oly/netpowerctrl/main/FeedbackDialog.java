package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import java.io.UnsupportedEncodingException;

import de.cketti.library.changelog.ChangeLog;
import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.Icons;

public class FeedbackDialog extends DialogFragment {

    public FeedbackDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressWarnings("ConstantConditions")
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.netpowerctrl);

        builder.setIcon(new BitmapDrawable(getResources(), Icons.resizeBitmap(getActivity(), b)));
        builder.setTitle(R.string.app_name);

        // Support this app by a donations.
        String[] items = getResources().getStringArray(R.array.feedbackOptions);
        try {
            items[items.length - 1] = items[items.length - 1].replace("%s",
                    new String(Base64.decode("UGF5UGFs", Base64.DEFAULT), "UTF-8"));
        } catch (Exception ignored) {
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: { // mail
                        @SuppressWarnings("ConstantConditions")
                        ApplicationInfo info = getActivity().getApplicationContext().getApplicationInfo();
                        PackageManager pm = getActivity().getApplicationContext().getPackageManager();
                        assert pm != null;
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"david.graeff@web.de"});
                        try {
                            assert info != null;
                            //noinspection ConstantConditions
                            intent.putExtra(Intent.EXTRA_SUBJECT, info.loadLabel(pm).toString() + "(" + pm.getPackageInfo(info.packageName, 0).versionName + ")" + " Contact Form | Device: " + Build.MANUFACTURER + " " + Build.DEVICE + "(" + Build.MODEL + ") API: " + Build.VERSION.SDK_INT);
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }
                        intent.setType("plain/html");
                        getActivity().startActivity(intent);
                    }
                    break;
                    case 1: { // bugtracker
                        @SuppressWarnings("ConstantConditions")
                        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl/issues"));
                        getActivity().startActivity(browse);
                    }
                    break;
                    case 2: {
                        @SuppressWarnings("ConstantConditions")
                        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl"));
                        getActivity().startActivity(browse);
                    }
                    break;
                    case 3: {
                        @SuppressWarnings("ConstantConditions")
                        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName()));
                        getActivity().startActivity(browse);
                    }
                    break;
                    case 4: {
                        ChangeLog cl = new ChangeLog(getActivity());
                        cl.getFullLogDialog().show();
                    }
                    break;
                    case 5: {
                        String p1 = "aHR0cHM6Ly93d3cucGF5cGFsLmNvbS9jZ2ktYmluL3dlYnNjcj9jbWQ9X3MteGNsaWNrJmhvc3RlZF9idXR0b25faWQ9OTNUQUFUSkIzV0pGMg==";
                        try {
                            String text = new String(Base64.decode(p1, Base64.DEFAULT), "UTF-8");
                            Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
                            getActivity().startActivity(browse);
                        } catch (UnsupportedEncodingException ignored) {
                        }
                    }
                    break;
                }
            }
        });

        return builder.create();
    }
}
