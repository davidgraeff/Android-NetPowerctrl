package oly.netpowerctrl.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class IconThemeDialog extends DialogFragment {
    public IconThemeDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_icon_theme, null, false);

        final SoftRadioGroup radioGroupTheme = new SoftRadioGroup();
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme1));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme2));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme3));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme4));

        String[] icon_themes = getResources().getStringArray(R.array.default_fallback_icon_set_keys);
        int[] icon_theme_layouts = new int[]{R.id.theme1_preview, R.id.theme2_preview, R.id.theme3_preview, R.id.theme4_preview};

        for (int i = 0; i < icon_theme_layouts.length; ++i) {
            try {
                View view = rootView.findViewById(icon_theme_layouts[i]);
                ((ImageView) view.findViewById(R.id.image_off)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateOff, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_on)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateOn, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_unknown)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateUnknown, icon_themes[i]));
                final int finalI = i;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        radioGroupTheme.check(finalI);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int index = radioGroupTheme.getCheckedRadioButtonIndex();
                        if (index != -1) {
                            String[] icon_themes = getResources().getStringArray(R.array.default_fallback_icon_set_keys);
                            LoadStoreIconData.setDefaultFallbackIconSet(icon_themes[index]);
                        }
                    }
                });
        return builder.create();
    }
}
