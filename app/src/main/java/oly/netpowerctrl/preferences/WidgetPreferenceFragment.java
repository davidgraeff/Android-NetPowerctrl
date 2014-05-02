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
    private int widgetId = -1;
    private final Map<Preference, Icons.IconState> preference_to_state = new TreeMap<Preference, Icons.IconState>();

    private final Preference.OnPreferenceClickListener selectImage = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(final Preference preference) {
            Icons.show_select_icon_dialog(getActivity(), "widget_icons", WidgetPreferenceFragment.this, preference);
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
        preference.setIcon(Icons.loadDrawable(getActivity(), Icons.uuidFromWidgetID(widgetId),
                Icons.IconType.WidgetIcon, Icons.IconState.StateOn,
                Icons.getResIdForState(Icons.IconState.StateOn)));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_off");
        assert preference != null;
        preference_to_state.put(preference, Icons.IconState.StateOff);
        preference.setIcon(Icons.loadDrawable(getActivity(), Icons.uuidFromWidgetID(widgetId),
                Icons.IconType.WidgetIcon, Icons.IconState.StateOff,
                Icons.getResIdForState(Icons.IconState.StateOff)));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_not_reachable");
        assert preference != null;
        preference_to_state.put(preference, Icons.IconState.StateUnknown);
        preference.setIcon(Icons.loadDrawable(getActivity(), Icons.uuidFromWidgetID(widgetId),
                Icons.IconType.WidgetIcon, Icons.IconState.StateUnknown,
                Icons.getResIdForState(Icons.IconState.StateUnknown)));
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
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null)
            return;
        Preference current = (Preference) context_object;
        Icons.IconState state = preference_to_state.get(context_object);
        Icons.saveIcon(getActivity(), Icons.uuidFromWidgetID(widgetId), bitmap, Icons.IconType.WidgetIcon, state);
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
