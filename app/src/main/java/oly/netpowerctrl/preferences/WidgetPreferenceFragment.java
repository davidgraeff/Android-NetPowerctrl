package oly.netpowerctrl.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

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
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        int PICK_IMAGE = 1;
                        current_preference = preference;
                        startActivityForResult(Intent.createChooser(intent, "Select Widget Icon"), PICK_IMAGE);
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

    public Drawable loadIcon(String key) {
        String uriString = getPreferenceManager().getSharedPreferences().getString(key, null);
        String filePath = "";
        if (uriString != null && uriString.length() > 0) {
            Uri selectedImage = Uri.parse(uriString);
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        }

        if (filePath == null || filePath.isEmpty() || !new File(filePath).exists()) {
            if (key.equals("widget_image_on"))
                return (getResources().getDrawable(R.drawable.widgeton));
            else if (key.equals("widget_image_off"))
                return (getResources().getDrawable(R.drawable.widgetoff));
            else if (key.equals("widget_image_not_reachable"))
                return (getResources().getDrawable(R.drawable.widgetunknown));
            else
                return null;
        } else {
            Bitmap bg;
            int cvwidth = 128;
            int cvheight = 128;
            BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inJustDecodeBounds = true;
            bg = BitmapFactory.decodeFile(filePath, options);
            if (bg == null) {
                Log.w("WidgetPreferenceFragment", "bg not found: " + filePath);
                getPreferenceManager().getSharedPreferences().edit().putString(key, "").commit();
                return loadIcon(key);
            }
            bg = Bitmap.createScaledBitmap(bg, cvwidth, cvheight, true);
            return new BitmapDrawable(getResources(), bg);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            getPreferenceManager().getSharedPreferences().edit().putString(current_preference.getKey(),
                    selectedImage.toString()).commit();
            current_preference.setIcon(loadIcon(current_preference.getKey()));
        }
    }
}
