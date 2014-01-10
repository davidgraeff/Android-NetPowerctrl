package oly.netpowerctrl.preferences;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;

import java.util.Set;

/**
 * Created by david on 04.01.14.
 */
public class PreferencesWithValuesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        initSummary();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //update summary
        updatePrefsSummary(sharedPreferences, findPreference(key));
    }

    /**
     * Update summary
     *
     * @param sharedPreferences
     * @param pref
     */
    protected void updatePrefsSummary(SharedPreferences sharedPreferences,
                                      Preference pref) {

        if (pref == null)
            return;

        if (pref instanceof ListPreference) {
            // List Preference
            ListPreference listPref = (ListPreference) pref;
            listPref.setSummary(listPref.getEntry());

        } else if (pref instanceof EditTextPreference) {
            // EditPreference
            EditTextPreference editTextPref = (EditTextPreference) pref;
            editTextPref.setSummary(editTextPref.getText());

        } else if (pref instanceof MultiSelectListPreference) {
            // MultiSelectList Preference
            MultiSelectListPreference mlistPref = (MultiSelectListPreference) pref;
            String summaryMListPref = "";
            String and = "";

            // Retrieve values
            Set<String> values = mlistPref.getValues();
            for (String value : values) {
                // For each value retrieve id
                int index = mlistPref.findIndexOfValue(value);
                // Retrieve entry from id
                CharSequence mEntry = index >= 0
                        && mlistPref.getEntries() != null ? mlistPref
                        .getEntries()[index] : null;
                if (mEntry != null) {
                    // add summary
                    summaryMListPref = summaryMListPref + and + mEntry;
                    and = ";";
                }
            }
            // set summary
            mlistPref.setSummary(summaryMListPref);

        } else if (pref instanceof RingtonePreference) {
            // RingtonePreference
            RingtonePreference rtPref = (RingtonePreference) pref;
            String uri;
            if (rtPref != null) {
                uri = sharedPreferences.getString(rtPref.getKey(), null);
                if (uri != null) {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            getActivity(), Uri.parse(uri));
                    pref.setSummary(ringtone.getTitle(getActivity()));
                }
            }
        }
//         else if (pref instanceof NumberPickerPreference) {
//            // My NumberPicker Preference
//            NumberPickerPreference nPickerPref = (NumberPickerPreference) pref;
//            nPickerPref.setSummary(nPickerPref.getValue());
//        }
    }

    /*
         * Init summary
         */
    protected void initSummary() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initPrefsSummary(getPreferenceManager().getSharedPreferences(),
                    getPreferenceScreen().getPreference(i));
        }
    }

    /*
         * Init single Preference
         */
    protected void initPrefsSummary(SharedPreferences sharedPreferences,
                                    Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) p;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                initPrefsSummary(sharedPreferences, pCat.getPreference(i));
            }
        } else {
            updatePrefsSummary(sharedPreferences, p);
        }
    }
}
