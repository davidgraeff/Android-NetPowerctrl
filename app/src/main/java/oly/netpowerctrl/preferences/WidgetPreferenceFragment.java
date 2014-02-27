package oly.netpowerctrl.preferences;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.widget.WidgetUpdateService;

public class WidgetPreferenceFragment extends PreferencesWithValuesFragment implements Icons.IconSelected {
    int widgetId = -1;
    Preference current;
    Map<Preference, Icons.IconState> preference_to_state = new TreeMap<Preference, Icons.IconState>();

    private Preference.OnPreferenceClickListener selectImage = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(final Preference preference) {
            current = preference;
            Icons.show_select_icon_dialog(getActivity(), "widget_icons", WidgetPreferenceFragment.this);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        assert arguments != null;
        widgetId = arguments.getInt("widgetId");
        //noinspection ConstantConditions
        getPreferenceManager().setSharedPreferencesName(arguments.getString("key"));
        addPreferencesFromResource(R.xml.preferences_widget);

        Preference preference;

        preference = findPreference("widget_image_on");
        assert preference != null;
        preference_to_state.put(preference, Icons.IconState.StateOn);
        preference.setIcon(Icons.loadStateIconDrawable(getActivity(), Icons.IconState.StateOn,
                Icons.uuidFromWidgetID(widgetId, Icons.IconState.StateOn)));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_off");
        assert preference != null;
        preference_to_state.put(preference, Icons.IconState.StateOff);
        preference.setIcon(Icons.loadStateIconDrawable(getActivity(), Icons.IconState.StateOff,
                Icons.uuidFromWidgetID(widgetId, Icons.IconState.StateOff)));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_not_reachable");
        assert preference != null;
        preference_to_state.put(preference, Icons.IconState.StateUnknown);
        preference.setIcon(Icons.loadStateIconDrawable(getActivity(), Icons.IconState.StateUnknown,
                Icons.uuidFromWidgetID(widgetId, Icons.IconState.StateUnknown)));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_show_text");
        assert preference != null;
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                WidgetUpdateService.ForceUpdate(getActivity(), widgetId);
                return false;
            }
        });
    }

    @Override
    public void setIcon(Bitmap bitmap) {
        Icons.IconState state = preference_to_state.get(current);
        Icons.saveIcon(getActivity(), Icons.uuidFromWidgetID(widgetId, state), bitmap, Icons.IconType.WidgetIcon);
        if (bitmap == null) {
            current.setIcon(Icons.getResIdForState(state));
        } else {
            current.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
        WidgetUpdateService.ForceUpdate(getActivity(), widgetId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Icons.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

}
