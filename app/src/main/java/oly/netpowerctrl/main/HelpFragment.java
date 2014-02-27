package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import oly.netpowerctrl.R;

public class HelpFragment extends Fragment {

    public HelpFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_help, container, false);
        TextView txtSubTitle;
        txtSubTitle = (TextView) v.findViewById(R.id.help_nfc);
        txtSubTitle.setText(Html.fromHtml(getResources().getString(R.string.help_nfc)));
        txtSubTitle = (TextView) v.findViewById(R.id.help_backup);
        txtSubTitle.setText(Html.fromHtml(getResources().getString(R.string.help_backup)));
        return v;
    }
}
