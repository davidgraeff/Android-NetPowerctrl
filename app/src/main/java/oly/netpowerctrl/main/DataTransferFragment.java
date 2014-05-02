package oly.netpowerctrl.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import oly.netpowerctrl.R;

public class DataTransferFragment extends Fragment {
    private class NFC_Help_Fragment extends Fragment {
        public NFC_Help_Fragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_help, container, false);

            assert v != null;
            TextView txtSubTitle = (TextView) v.findViewById(R.id.help);
            txtSubTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.nfc_checkpoint, 0, 0, 0);
            txtSubTitle.setText(Html.fromHtml(getResources().getString(R.string.help_nfc)));

            Button btn = (Button) v.findViewById(R.id.button);
            btn.setText(R.string.activate_nfc);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent setnfc = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivity(setnfc);
                }
            });

            return v;
        }
    }

    private class GoogleDrive_Help_Fragment extends Fragment {
        public GoogleDrive_Help_Fragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_help, container, false);

            assert v != null;
            TextView txtSubTitle = (TextView) v.findViewById(R.id.help);
            txtSubTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.data_backup, 0, 0, 0);
            txtSubTitle.setText(Html.fromHtml(getResources().getString(R.string.help_backup)));
            return v;
        }
    }

    /**
     * This adapter is for the FragmentPager to show two OutletsFragments
     * (Available actions, scene included actions)
     */
    private class Pager_Adapter extends FragmentPagerAdapter {
        private final Fragment[] frag;

        public Pager_Adapter(FragmentManager fm) {
            super(fm);

            NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (mNfcAdapter != null) {
                Fragment f1 = new NFC_Help_Fragment();
                Fragment f2 = new Fragment();
                Fragment f3 = new GoogleDrive_Help_Fragment();
                frag = new Fragment[]{f1, f2, f3};
            } else { // No NFC page
                Fragment f2 = new Fragment();
                Fragment f3 = new GoogleDrive_Help_Fragment();
                frag = new Fragment[]{f2, f3};
            }
        }

        @Override
        public Fragment getItem(int i) {
            return frag[i];
        }

        @Override
        public int getCount() {
            return frag.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "NFC";
                case 1:
                    return "Nachbarn";
                case 2:
                    return "Google Drive";
            }
            return "";
        }
    }

    public DataTransferFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data_transfer, container, false);

        // ViewPager init
        assert v != null;
        ViewPager pager = (ViewPager) v.findViewById(R.id.pager);
        pager.setAdapter(new Pager_Adapter(getFragmentManager()));
        return v;
    }
}
