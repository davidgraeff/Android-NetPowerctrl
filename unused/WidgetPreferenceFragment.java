package oly.netpowerctrl.preferences;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.widget.WidgetUpdateService;

public class WidgetPreferenceFragment extends PreferencesWithValuesFragment implements LoadStoreIconData.IconSelected {
    private final Map<Preference, LoadStoreIconData.IconState> preference_to_state = new TreeMap<>();
    private final Preference.OnPreferenceClickListener selectImage = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(final Preference preference) {
            LoadStoreIconData.show_select_icon_dialog(getActivity(), "widget_icons", WidgetPreferenceFragment.this, preference);
            return true;
        }
    };
    private int widgetId = -1;
    private final Preference.OnPreferenceChangeListener forceChangePreferences =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    App.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (widgetId == -1) {
                                WidgetUpdateService.ForceUpdateAll(getActivity());
                            } else
                                WidgetUpdateService.ForceUpdate(getActivity(), widgetId);
                        }
                    }, 200);
                    return true;
                }
            };
    private String widget_uuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        assert arguments != null;
        widgetId = arguments.getInt("widgetId");
        //noinspection ConstantConditions
        getPreferenceManager().setSharedPreferencesName(arguments.getString("key"));
        addPreferencesFromResource(R.xml.preferences_widget);

        widget_uuid = (widgetId == -1) ? LoadStoreIconData.uuidFromDefaultWidget() : LoadStoreIconData.uuidFromWidgetID(widgetId);

        Preference preference;

        preference = findPreference("widget_use_default");
        if (widgetId == -1) {
            getPreferenceScreen().removePreference(preference);
        }

        preference = findPreference("widget_image_on");
        assert preference != null;
        if (widgetId != -1)
            preference.setDependency("widget_use_default");
        preference_to_state.put(preference, LoadStoreIconData.IconState.StateOn);
        preference.setIcon(LoadStoreIconData.loadDrawable(getActivity(), widget_uuid,
                LoadStoreIconData.IconType.WidgetIcon, LoadStoreIconData.IconState.StateOn, null));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_off");
        assert preference != null;
        if (widgetId != -1)
            preference.setDependency("widget_use_default");
        preference_to_state.put(preference, LoadStoreIconData.IconState.StateOff);
        preference.setIcon(LoadStoreIconData.loadDrawable(getActivity(), widget_uuid,
                LoadStoreIconData.IconType.WidgetIcon, LoadStoreIconData.IconState.StateOff, null));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_not_reachable");
        assert preference != null;
        if (widgetId != -1)
            preference.setDependency("widget_use_default");
        preference_to_state.put(preference, LoadStoreIconData.IconState.StateUnknown);
        preference.setIcon(LoadStoreIconData.loadDrawable(getActivity(), widget_uuid,
                LoadStoreIconData.IconType.WidgetIcon, LoadStoreIconData.IconState.StateUnknown, null));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_show_title");
        assert preference != null;
        if (widgetId != -1)
            preference.setDependency("widget_use_default");
        preference.setOnPreferenceChangeListener(forceChangePreferences);

        preference = findPreference("widget_show_status");
        assert preference != null;
        if (widgetId != -1)
            preference.setDependency("widget_use_default");
        preference.setOnPreferenceChangeListener(forceChangePreferences);

    }

    @Override
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null)
            return;
        Preference current = (Preference) context_object;
        LoadStoreIconData.IconState state = preference_to_state.get(context_object);

        LoadStoreIconData.saveIcon(getActivity(), bitmap, widget_uuid, LoadStoreIconData.IconType.WidgetIcon, state);
        if (bitmap == null) {
            current.setIcon(LoadStoreIconData.loadDrawable(getActivity(), widget_uuid,
                    LoadStoreIconData.IconType.WidgetIcon, state, null));
        } else {
            current.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
        WidgetUpdateService.ForceUpdate(getActivity(), widgetId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        //super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        LoadStoreIconData.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

}
