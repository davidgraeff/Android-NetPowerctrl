package oly.netpowerctrl.preferences;

import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.utils.Github;
import oly.netpowerctrl.widget.DeviceWidgetProvider;

public class PreferencesFragment extends PreferencesWithValuesFragment implements Github.IGithubOpenIssues {
    private final Preference.OnPreferenceChangeListener reloadActivity = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            //noinspection ConstantConditions
            getActivity().recreate();
            return true;
        }
    };

    @Override
    public void onPause() {
        ListView lv = (ListView) getActivity().findViewById(android.R.id.list);
        if (lv != null)
            getPreferenceManager().getSharedPreferences().edit().putInt("scroll", lv.getFirstVisiblePosition()).apply();
        super.onPause();
    }

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
                MainActivity.getNavigationController().changeToDialog(getActivity(), EnergySaveLogFragment.class.getName());
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    Toast.makeText(getActivity(), "This is only available for Android 4.4 and newer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 9876);
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(getActivity(), "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("export").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    Toast.makeText(getActivity(), "This is only available for Android 4.4 and newer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType(DocumentsContract.Document.MIME_TYPE_DIR)
                            .putExtra(Intent.EXTRA_TITLE, "test.bkp");
                    startActivityForResult(intent, 9875);
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(getActivity(), "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                }
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
            String prefName = SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId);
            String port_uuid = SharedPrefs.getInstance().LoadWidget(appWidgetId);
            if (port_uuid == null) {
                Log.e("PREFERENCES", "Loading widget failed: " + String.valueOf(appWidgetId));
                continue;
            }
            DevicePort port = AppData.getInstance().findDevicePort(
                    UUID.fromString(port_uuid));
            if (port == null) {
                Log.e("PREFERENCES", "Port for widget not found: " + String.valueOf(appWidgetId));
                continue;
            }
            widgetDataList.add(new WidgetData(
                    port.device.DeviceName + ", " + port.getDescription(),
                    prefName, appWidgetId));
        }

        //noinspection ConstantConditions
        findPreference("all_widgets").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Bundle extra = new Bundle();
                extra.putString("key", SharedPrefs.PREF_WIDGET_BASENAME);
                extra.putInt("widgetId", -1);
                MainActivity.getNavigationController().changeToFragment(WidgetPreferenceFragment.class.getName(), extra, true);
                return false;
            }
        });

        PreferenceCategory lp = (PreferenceCategory) findPreference(SharedPrefs.PREF_widgets);
        assert lp != null;

        for (final WidgetData aWidgetDataList : widgetDataList) {
            //noinspection ConstantConditions
            PreferenceScreen s = getPreferenceManager().createPreferenceScreen(getActivity());
            assert s != null;
            s.setKey(aWidgetDataList.prefName);
            s.setFragment(WidgetPreferenceFragment.class.getName());
            s.setTitle(aWidgetDataList.data);
            s.setIcon(LoadStoreIconData.loadDrawable(getActivity(), LoadStoreIconData.uuidFromWidgetID(aWidgetDataList.widgetID),
                    LoadStoreIconData.IconType.WidgetIcon, LoadStoreIconData.IconState.StateOn,
                    LoadStoreIconData.getResIdForState(LoadStoreIconData.IconState.StateOn)));
            lp.addPreference(s);
            s.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey() == null || preference.getKey().isEmpty())
                        return true;
                    Bundle extra = new Bundle();
                    extra.putString("key", preference.getKey());
                    extra.putInt("widgetId", aWidgetDataList.widgetID);
                    MainActivity.getNavigationController().changeToFragment(preference.getFragment(), extra, true);
                    return true;
                }
            });
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
        final ListView lv = (ListView) getActivity().findViewById(android.R.id.list);
        if (lv != null)
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    lv.setSelection(getPreferenceManager().getSharedPreferences().getInt("scroll", 0));
                }
            });
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
        final CharSequence data;
        final String prefName;
        final int widgetID;

        private WidgetData(CharSequence data, String prefName, int widgetID) {
            this.data = data;
            this.prefName = prefName;
            this.widgetID = widgetID;
        }
    }
}
