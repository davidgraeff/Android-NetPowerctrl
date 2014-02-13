package oly.netpowerctrl.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.Preference;

import java.io.FileDescriptor;
import java.io.IOException;

import oly.netpowerctrl.R;

public class WidgetPreferenceFragment extends PreferencesWithValuesFragment {
    private Preference current_preference;
    private Preference.OnPreferenceClickListener selectImage = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(final Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Widget Icon");
            builder.setItems(new String[]{"Default Icon", "Select Widget Icon"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        getPreferenceManager().getSharedPreferences().edit().putString(preference.getKey(), "").commit();
                        preference.setIcon(loadIcon(preference.getKey()));
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        int PICK_IMAGE = 1;
                        current_preference = preference;
                        startActivityForResult(intent, PICK_IMAGE);
                    }
                    dialogInterface.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
    };

    public static Uri getURI(Context context, int appWidgetId, String state) {
        String stringUri = context.getSharedPreferences(SharedPrefs.PREF_WIDGET_BASENAME + String.valueOf(appWidgetId),
                Context.MODE_PRIVATE).getString(state, null);
        if (stringUri == null || stringUri.isEmpty()) {
            if (state.equals("widget_image_on"))
                return Uri.parse("android.resource://" + context.getApplicationInfo().packageName + "/" + R.drawable.widgeton);
            else if (state.equals("widget_image_off"))
                return Uri.parse("android.resource://" + context.getApplicationInfo().packageName + "/" + R.drawable.widgetoff);
            else if (state.equals("widget_image_not_reachable"))
                return Uri.parse("android.resource://" + context.getApplicationInfo().packageName + "/" + R.drawable.widgetunknown);
            return null;
        }

        return Uri.parse(stringUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getArguments().getString("key"));
        addPreferencesFromResource(R.xml.preferences_widget);

        Preference preference;

        preference = findPreference("widget_image_on");
        preference.setIcon(loadIcon(preference.getKey()));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_off");
        preference.setIcon(loadIcon(preference.getKey()));
        preference.setOnPreferenceClickListener(selectImage);

        preference = findPreference("widget_image_not_reachable");
        preference.setIcon(loadIcon(preference.getKey()));
        preference.setOnPreferenceClickListener(selectImage);
    }

    private Drawable getDrawableFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getActivity().getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return new BitmapDrawable(getResources(), image);
    }

    public Drawable loadIcon(String key) {
        String uriString = getPreferenceManager().getSharedPreferences().getString(key, null);
        Drawable dest = null;
        if (uriString != null && uriString.length() > 0) {
            try {
                dest = getDrawableFromUri(Uri.parse(uriString));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (dest == null) {
            if (key.equals("widget_image_on"))
                return (getResources().getDrawable(R.drawable.widgeton));
            else if (key.equals("widget_image_off"))
                return (getResources().getDrawable(R.drawable.widgetoff));
            else if (key.equals("widget_image_not_reachable"))
                return (getResources().getDrawable(R.drawable.widgetunknown));
        }
        return dest;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            final int takeFlags = imageReturnedIntent.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Check for the freshest data.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                getActivity().getContentResolver().takePersistableUriPermission(selectedImage, takeFlags);

            getPreferenceManager().getSharedPreferences().edit().putString(current_preference.getKey(),
                    selectedImage.toString()).commit();
            current_preference.setIcon(loadIcon(current_preference.getKey()));
        }
    }
}
