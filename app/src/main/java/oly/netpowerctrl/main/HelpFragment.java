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
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

public class HelpFragment extends Fragment {

	public HelpFragment() {
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        WebView view = new WebView(container.getContext());
        Resources res = getResources();
        try {
            InputStream in_s = res.openRawResource(R.raw.help);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            view.loadData((new String(b)).replace('\n',' '), "text/html", null);
        } catch (Exception e) {
            // nop
        }
        return view;
    }
}
