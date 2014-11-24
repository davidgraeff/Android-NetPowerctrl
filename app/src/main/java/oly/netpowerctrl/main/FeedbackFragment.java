package oly.netpowerctrl.main;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.ChangeLogUtil;
import oly.netpowerctrl.utils.GithubAndCloudant;

public class FeedbackFragment extends Fragment {
    GithubAndCloudant githubAndCloudant = new GithubAndCloudant();

    public FeedbackFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        assert view != null;

        String version = "";
        try {
            //noinspection ConstantConditions
            version += getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        TextView versionText = ((TextView) view.findViewById(R.id.version));
        versionText.setText(version);
        versionText.setPaintFlags(versionText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        versionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChangeLogUtil.showChangeLog(getActivity());
            }
        });

        ((TextView) view.findViewById(R.id.bugs)).setText(R.string.issues_wait);
        githubAndCloudant.getOpenAutomaticIssues(new GithubAndCloudant.IGithubOpenIssues() {
            @Override
            public void gitHubOpenIssuesUpdated(int count, long last_access) {
                ((TextView) view.findViewById(R.id.bugs)).setText(String.valueOf(count));
            }

            @Override
            public void gitHubIssue(int number, String title, String body) {

            }
        }, false);

        ((TextView) view.findViewById(R.id.bugs2)).setText(R.string.issues_wait);
        githubAndCloudant.getOpenIssues(new GithubAndCloudant.IGithubOpenIssues() {
            @Override
            public void gitHubOpenIssuesUpdated(int count, long last_access) {
                ((TextView) view.findViewById(R.id.bugs2)).setText(String.valueOf(count));
            }

            @Override
            public void gitHubIssue(int number, String title, String body) {

            }
        }, false, null);

        view.findViewById(R.id.mail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });

        view.findViewById(R.id.google_market).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + getActivity().getPackageName()));
                getActivity().startActivity(browse);
            }
        });

        view.findViewById(R.id.source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl"));
                getActivity().startActivity(browse);
            }
        });

        view.findViewById(R.id.open_source_license).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidgraeff/Android-NetPowerctrl/wiki/used_software"));
                getActivity().startActivity(browse);
            }
        });

        view.findViewById(R.id.feedback_google_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/100828661972389152711"));
                getActivity().startActivity(browse);
            }
        });

        view.findViewById(R.id.beta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/apps/testing/oly.netpowerctrl"));
                getActivity().startActivity(browse);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
