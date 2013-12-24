package oly.netpowerctrl.main;

import java.io.InputStream;
import oly.netpowerctrl.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {

	public AboutDialog() {
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about, container, false);
        TextView tvAboutTitle = (TextView)view.findViewById(R.id.tvAboutTitle);
        tvAboutTitle.setText(ReadHTMLResource(R.raw.about));
        Button tvAboutStore = (Button)view.findViewById(R.id.tvAboutStore);
        tvAboutStore.setText(getResources().getString(R.string.rate_and_comment));
        tvAboutStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( "market://details?id="+getActivity().getPackageName() ) );
                getActivity().startActivity(browse);
            }
        });
        Button tvAboutSource = (Button)view.findViewById(R.id.tvAboutSource);
        tvAboutSource.setText(getResources().getString(R.string.bug_reports));
        tvAboutSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( "https://github.com/davidgraeff/netpowerctrl-sf-mirror" ) );
                getActivity().startActivity(browse);
            }
        });
        return view;
    }

    public Spanned ReadHTMLResource(int id) {
	    try {
	        Resources res = getResources();
	        InputStream in_s = res.openRawResource(id);
	        byte[] b = new byte[in_s.available()];
	        in_s.read(b);
	        PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
	        String s = new String(b);
	        s = s.replace("VersionX", pInfo.versionName);
	        return Html.fromHtml(s);
	    } catch (Exception e) {
	    	// nop
	    }
		return new SpannableString("");
    }

}
