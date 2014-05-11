package oly.netpowerctrl.preferences;

import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.widget.DeviceWidgetProvider;

public class PreferencesFragment extends PreferencesWithValuesFragment {
    private static class WidgetData {
        CharSequence data;
        String prefName;

        private WidgetData(CharSequence data, String prefName) {
            this.data = data;
            this.prefName = prefName;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.PREF_use_dark_theme).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //noinspection ConstantConditions
                getActivity().recreate();
                return true;
            }
        });

        //noinspection ConstantConditions
        findPreference("open_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //noinspection ConstantConditions
                Fragment fragment = Fragment.instantiate(getActivity(), EnergySaveLogFragment.class.getName());
                //noinspection ConstantConditions
                getFragmentManager().beginTransaction().addToBackStack(null).
                        replace(R.id.content_frame, fragment).commit();
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("use_log_energy_saving_mode").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(), getString(R.string.log_activated), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        //noinspection ConstantConditions
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        ComponentName deviceWidgetWidget = new ComponentName(getActivity(),
                DeviceWidgetProvider.class);
        //noinspection ConstantConditions
        final int[] allWidgetIds = appWidgetManager.getAppWidgetIds(deviceWidgetWidget);
        List<WidgetData> widgetDataList = new ArrayList<>();
        int index = 0;
        for (int appWidgetId : allWidgetIds) {
            String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            String port_uuid = SharedPrefs.LoadWidget(appWidgetId);
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(
                    port_uuid == null ? null : UUID.fromString(port_uuid));
            if (port == null) {
                Log.w("PREFERENCES", "Strange widget ID!");
                continue;
            }
            widgetDataList.add(new WidgetData(
                    port.device.DeviceName + ", " + port.getDescription(),
                    prefName));
            ++index;
        }

        PreferenceCategory lp = (PreferenceCategory) findPreference(SharedPrefs.PREF_widgets);
        assert lp != null;
        if (widgetDataList.isEmpty()) {
            getPreferenceScreen().removePreference(lp);
        } else {
            for (int i = 0; i < widgetDataList.size(); ++i) {
                final int widgetID = allWidgetIds[i];
                //noinspection ConstantConditions
                PreferenceScreen s = getPreferenceManager().createPreferenceScreen(getActivity());
                assert s != null;
                s.setKey(widgetDataList.get(i).prefName);
                s.setFragment(WidgetPreferenceFragment.class.getName());
                s.setTitle(widgetDataList.get(i).data);
                s.setIcon(Icons.loadDrawable(getActivity(), Icons.uuidFromWidgetID(widgetID),
                        Icons.IconType.WidgetIcon, Icons.IconState.StateOn,
                        Icons.getResIdForState(Icons.IconState.StateOn)));
                lp.addPreference(s);
                s.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (preference.getKey() == null || preference.getKey().isEmpty())
                            return true;
                        Fragment fragment = Fragment.instantiate(getActivity(), preference.getFragment());
                        Bundle b = new Bundle();
                        b.putString("key", preference.getKey());
                        b.putInt("widgetId", widgetID);
                        fragment.setArguments(b);
                        //noinspection ConstantConditions
                        getFragmentManager().beginTransaction().addToBackStack(null).
                                replace(R.id.content_frame, fragment).commit();
                        return true;
                    }
                });
            }
        }
    }
}
