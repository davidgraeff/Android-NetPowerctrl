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
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.ChangeLogUtil;
import oly.netpowerctrl.utils.Github;

public class FeedbackFragment extends Fragment implements Github.IGithubNewIssue {
    private LinearLayout wishes_layout;
    private Map<Integer, View> number_to_view = new TreeMap<>();

    public FeedbackFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        assert view != null;

        String version = getString(R.string.Version) + " ";
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

        wishes_layout = (LinearLayout) view.findViewById(R.id.layout);

        view.findViewById(R.id.issue_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String t = "{\n" +
                        "  \"title\": \"Found a bug\",\n" +
                        "  \"body\": \"I'm having a problem with this.\",\n" +
                        "  \"labels\": [\n" +
                        "    \"enhancement\"\n" +
                        "  ]\n" +
                        "}";
                Github.newIssues(t, FeedbackFragment.this);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Github.getOpenIssues(this, true, "enhancement");
    }

    @Override
    public void gitHubOpenIssuesUpdated(int count, long last_access) {

    }

    @Override
    public void gitHubIssue(int number, String title, String body) {
        CardView.LayoutParams l = new CardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        @SuppressWarnings("ConstantConditions")
        LayoutInflater li = LayoutInflater.from(getView().getContext());
        View v = li.inflate(R.layout.fragment_feedback_add, wishes_layout, false);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        ((TextView) v.findViewById(R.id.text)).setText(body);
        ((Button) v.findViewById(R.id.issue_add)).setText(R.string.feedback_btn_edit);
        number_to_view.put(number, v);

        wishes_layout.addView(v, l);
    }

    @Override
    public void newIssueResponse(boolean success) {
        if (!success)
            Toast.makeText(getActivity(), R.string.feedback_add_failed, Toast.LENGTH_SHORT).show();
    }
}
