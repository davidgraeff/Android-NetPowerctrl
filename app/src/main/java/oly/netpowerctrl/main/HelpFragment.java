package oly.netpowerctrl.main;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.io.InputStream;

import oly.netpowerctrl.R;

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
            //noinspection ResultOfMethodCallIgnored
            in_s.read(b);
            view.loadData((new String(b)).replace('\n',' '), "text/html", null);
        } catch (Exception e) {
            // nop
        }
        return view;
    }
}
