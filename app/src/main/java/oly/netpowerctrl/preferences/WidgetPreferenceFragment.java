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

public class WidgetPreferenceFragment extends PreferencesWithValuesFragment implements Icons.IconSelected {
    int widgetId = -1;
    Preference current;
    Map<Preference, Icons.WidgetState> preference_to_state = new TreeMap<Preference, Icons.WidgetState>();

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

        widgetId = getArguments().getInt("widgetId");
        getPreferenceManager().setSharedPreferencesName(getArguments().getString("key"));
        addPreferencesFromResource(R.xml.preferences_widget);

        Preference preference;

        preference = findPreference("widget_image_on");
        preference_to_state.put(preference, Icons.WidgetState.WidgetOn);
        preference.setIcon(Icons.loadWidgetIcon(getActivity(), Icons.WidgetState.WidgetOn, widgetId));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_off");
        preference_to_state.put(preference, Icons.WidgetState.WidgetOff);
        preference.setIcon(Icons.loadWidgetIcon(getActivity(), Icons.WidgetState.WidgetOff, widgetId));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_not_reachable");
        preference_to_state.put(preference, Icons.WidgetState.WidgetUnknown);
        preference.setIcon(Icons.loadWidgetIcon(getActivity(), Icons.WidgetState.WidgetUnknown, widgetId));
        preference.setOnPreferenceClickListener(selectImage);
    }

    @Override
    public void setIcon(Bitmap bitmap) {
        Icons.WidgetState state = preference_to_state.get(current);
        Icons.saveIcon(getActivity(), Icons.uuidFromWidgetID(widgetId, state), bitmap, Icons.IconType.WidgetIcon);
        if (bitmap == null) {
            current.setIcon(Icons.loadWidgetIconFromRes(state));
        } else {
            current.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Icons.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

}
