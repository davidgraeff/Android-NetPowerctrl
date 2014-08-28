package oly.netpowerctrl.preferences;

import android.app.Fragment;
import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.utils.Github;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.widget.DeviceWidgetProvider;

public class PreferencesFragment extends PreferencesWithValuesFragment implements Github.IGithubOpenIssues {
    Preference.OnPreferenceChangeListener reloadActivity = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            //noinspection ConstantConditions
            getActivity().recreate();
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //noinspection ConstantConditions
        findPreference(SharedPrefs.getInstance().PREF_use_dark_theme).setOnPreferenceChangeListener(reloadActivity);
        findPreference("show_background").setOnPreferenceChangeListener(reloadActivity);

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
//
//        //noinspection ConstantConditions
//        findPreference("show_extensions").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                @SuppressWarnings("ConstantConditions")
//                Intent browse = new Intent(Intent.ACTION_VIEW,
//                        Uri.parse("market://search?q=pub:David Gräff&c=apps"));
//                getActivity().startActivity(browse);
//                return false;
//            }
//        });

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
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(deviceWidgetWidget);
        List<WidgetData> widgetDataList = new ArrayList<>();

        for (int appWidgetId : allWidgetIds) {
            String prefName = SharedPrefs.getInstance().PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            String port_uuid = SharedPrefs.getInstance().LoadWidget(appWidgetId);
            if (port_uuid == null) {
                Log.e("PREFERENCES", "Loading widget failed: " + String.valueOf(appWidgetId));
                continue;
            }
            DevicePort port = RuntimeDataController.getDataController().findDevicePort(
                    UUID.fromString(port_uuid));
            if (port == null) {
                Log.e("PREFERENCES", "Port for widget not found: " + String.valueOf(appWidgetId));
                continue;
            }
            widgetDataList.add(new WidgetData(
                    port.device.DeviceName + ", " + port.getDescription(),
                    prefName, appWidgetId));
        }

        PreferenceCategory lp = (PreferenceCategory) findPreference(SharedPrefs.getInstance().PREF_widgets);
        assert lp != null;
        if (widgetDataList.isEmpty()) {
            getPreferenceScreen().removePreference(lp);
        } else {
            for (final WidgetData aWidgetDataList : widgetDataList) {
                //noinspection ConstantConditions
                PreferenceScreen s = getPreferenceManager().createPreferenceScreen(getActivity());
                assert s != null;
                s.setKey(aWidgetDataList.prefName);
                s.setFragment(WidgetPreferenceFragment.class.getName());
                s.setTitle(aWidgetDataList.data);
                s.setIcon(Icons.loadDrawable(getActivity(), Icons.uuidFromWidgetID(aWidgetDataList.widgetID),
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
                        b.putInt("widgetId", aWidgetDataList.widgetID);
                        fragment.setArguments(b);
                        //noinspection ConstantConditions
                        getFragmentManager().beginTransaction().addToBackStack(null).
                                replace(R.id.content_frame, fragment).commit();
                        return true;
                    }
                });
            }
        }

        //noinspection ConstantConditions
        findPreference("issues").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Github.getOpenIssues(PreferencesFragment.this, true);
                return false;
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Github.getOpenIssues(this, false);
    }

    @Override
    public void gitHubOpenIssuesUpdated(int count, long last_access) {
        Context context = getActivity();
        if (context == null)
            return;

        findPreference("issues").setTitle(context.getString(R.string.issues_open, count));
        if (count < 0) {
            findPreference("issues").setSummary(context.getString(R.string.issues_error));
            return;
        }
        final String date = DateFormat.getInstance().format(last_access);
        findPreference("issues").setSummary(context.getString(R.string.issues_last_access, date));
    }

    private static class WidgetData {
        CharSequence data;
        String prefName;
        int widgetID;

        private WidgetData(CharSequence data, String prefName, int widgetID) {
            this.data = data;
            this.prefName = prefName;
            this.widgetID = widgetID;
        }
    }
}
