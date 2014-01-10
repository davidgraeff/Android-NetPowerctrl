package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;

import oly.netpowerctrl.R;

public class PreferencesFragment extends PreferencesWithValuesFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_use_dark_theme).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.preference_change_the_title)
                        .setMessage(R.string.preference_change_the_message)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getActivity().recreate();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });
    }
}
