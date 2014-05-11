package oly.netpowerctrl.preferences;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import java.util.Set;

/**
 * A preference fragment where preferences show their values instead of the summary
 */
public class PreferencesWithValuesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes
        //noinspection ConstantConditions
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        initSummary();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        //noinspection ConstantConditions
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //update summary
        updatePrefsSummary(findPreference(key));
    }

    /**
     * Update summary
     *
     * @param pref the preference
     */
    void updatePrefsSummary(Preference pref) {

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
            MultiSelectListPreference listPref = (MultiSelectListPreference) pref;
            String summaryMListPref = "";
            String and = "";

            // Retrieve values
            Set<String> values = listPref.getValues();
            for (String value : values) {
                // For each value retrieve id
                int index = listPref.findIndexOfValue(value);
                // Retrieve entry from id
                CharSequence mEntry = index >= 0
                        && listPref.getEntries() != null ? listPref
                        .getEntries()[index] : null;
                if (mEntry != null) {
                    // add summary
                    summaryMListPref = summaryMListPref + and + mEntry;
                    and = ";";
                }
            }
            // set summary
            listPref.setSummary(summaryMListPref);

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
    void initSummary() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            //noinspection ConstantConditions
            initPrefsSummary(
                    getPreferenceScreen().getPreference(i));
        }
    }

    /*
         * Init single Preference
         */
    void initPrefsSummary(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) p;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                initPrefsSummary(pCat.getPreference(i));
            }
        } else {
            updatePrefsSummary(p);
        }
    }
}
