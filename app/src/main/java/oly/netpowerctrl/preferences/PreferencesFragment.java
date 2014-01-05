package oly.netpowerctrl.preferences;

import android.os.Bundle;

import oly.netpowerctrl.R;

public class PreferencesFragment extends PreferencesWithValuesFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
