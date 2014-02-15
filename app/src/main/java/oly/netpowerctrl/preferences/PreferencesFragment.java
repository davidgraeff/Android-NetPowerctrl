package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.widget.DeviceWidgetProvider;

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

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        ComponentName deviceWidgetWidget = new ComponentName(getActivity(),
                DeviceWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(deviceWidgetWidget);
        CharSequence[] entries = new CharSequence[allWidgetIds.length];
        String[] entryValues = new String[allWidgetIds.length];
        int index = 0;
        for (int appWidgetId : allWidgetIds) {
            String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            SharedPrefs.WidgetOutlet outlet = SharedPrefs.LoadWidget(appWidgetId);
            DeviceInfo di = null;
            if (outlet == null) {
                Log.w("PREFERENCES", "Strange widget ID!");
                continue;
            }
            di = NetpowerctrlApplication.getDataController().findDevice(outlet.deviceMac);
            if (di == null)
                entries[index] = outlet.deviceMac + ", " + outlet.outletNumber;
            else
                entries[index] = di.DeviceName + ", " + di.findOutlet(outlet.outletNumber).getDescription();
            entryValues[index] = prefName;
            ++index;
        }

        PreferenceCategory lp = (PreferenceCategory) findPreference(SharedPrefs.PREF_widgets);
        if (entries.length == 0) {
            getPreferenceScreen().removePreference(lp);
            return;
        } else {
            for (int i = 0; i < entries.length; ++i) {
                PreferenceScreen s = getPreferenceManager().createPreferenceScreen(getActivity());
                s.setKey(entryValues[i]);
                s.setFragment(WidgetPreferenceFragment.class.getName());
                s.setTitle(entries[i]);
                lp.addPreference(s);
                s.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (preference.getKey() == null || preference.getKey().isEmpty())
                            return true;
                        Fragment fragment = Fragment.instantiate(getActivity(), preference.getFragment());
                        Bundle b = new Bundle();
                        b.putString("key", preference.getKey());
                        fragment.setArguments(b);
                        getFragmentManager().beginTransaction().addToBackStack(null).
                                replace(R.id.content_frame, fragment).commit();
                        return true;
                    }
                });
            }
        }
    }
}
