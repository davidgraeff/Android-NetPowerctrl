package oly.netpowerctrl.preferences;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.ImportExport;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.main.NfcTagWriterActivity;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.statusbar_and_speech.AndroidStatusBarService;

public class PreferencesFragment extends PreferencesWithValuesFragment implements LoadStoreIconData.IconSelected {
    private static final int REQUEST_CODE_IMPORT = 100;
    private static final int REQUEST_CODE_EXPORT = 101;

    private final Preference.OnPreferenceChangeListener reloadActivity = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            LoadStoreIconData.clearIconCache();
            //noinspection ConstantConditions
            getActivity().recreate();
            return true;
        }
    };
    private final Preference.OnPreferenceChangeListener reloadAndroidStatusBar = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            AndroidStatusBarService.startOrStop(getActivity());
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
        findPreference(SharedPrefs.PREF_use_dark_theme).setOnPreferenceChangeListener(reloadActivity);
        findPreference(SharedPrefs.PREF_fullscreen).setOnPreferenceChangeListener(reloadActivity);
        findPreference(SharedPrefs.PREF_background).setOnPreferenceChangeListener(reloadActivity);

        //noinspection ConstantConditions
        findPreference("open_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MainActivity.getNavigationController().changeToFragment(LogFragment.class.getName());
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("show_persistent_notification").setOnPreferenceChangeListener(reloadAndroidStatusBar);

        //noinspection ConstantConditions
        findPreference("import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    Toast.makeText(getActivity(), "This is only available for Android 4.4 and newer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/zip");
                    startActivityForResult(intent, REQUEST_CODE_IMPORT);
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(getActivity(), "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("export").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    Toast.makeText(getActivity(), "This is only available for Android 4.4 and newer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/zip")
                            .putExtra(Intent.EXTRA_TITLE, getActivity().getPackageName() + "-" + Utils.getDateTime(getActivity()) + ".zip");
                    startActivityForResult(intent, REQUEST_CODE_EXPORT);
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(getActivity(), "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("select_background_image").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "backgrounds", new LoadStoreIconData.IconSelected() {
                    @Override
                    public void setIcon(Object context_object, Bitmap bitmap) {
                        PreferencesFragment.this.setIcon(context_object, bitmap);
                    }

                    @Override
                    public void startActivityForResult(Intent intent, int requestCode) {
                        PreferencesFragment.this.startActivityForResult(intent, requestCode);
                    }
                }, null);
                return false;
            }
        });

        //noinspection ConstantConditions
        findPreference("show_extensions").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                @SuppressWarnings("ConstantConditions")
                Intent browse = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://search?q=pub:David Gräff&c=apps"));
                getActivity().startActivity(browse);
                return false;
            }
        });

        if (NfcAdapter.getDefaultAdapter(getActivity()) == null) {
            getPreferenceScreen().removePreference(findPreference("nfc_bind"));
        } else {
            //noinspection ConstantConditions
            findPreference("nfc_bind").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), NfcTagWriterActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMPORT) {
                ImportExport.importData(getActivity(), intent.getData());
            } else if (requestCode == REQUEST_CODE_EXPORT) {
                ImportExport.exportData(getActivity(), intent.getData());
            } else
                LoadStoreIconData.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, intent);
        }
    }

    @Override
    public void setIcon(Object context_object, Bitmap bitmap) {
        LoadStoreIconData.saveBackground(bitmap);

        if (SharedPrefs.getInstance().isBackground())
            reloadActivity.onPreferenceChange(null, null);
    }
}
