package oly.netpowerctrl.preferences;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.rey.material.app.Dialog;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.IconState;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.ui.SoftRadioGroup;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class IconThemeDialog extends DialogFragment {
    private SoftRadioGroup radioGroupTheme;
    public IconThemeDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_icon_theme, container, false);

        radioGroupTheme = new SoftRadioGroup();
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
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), IconState.StateOff, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_on)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), IconState.StateOn, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_unknown)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), IconState.StateUnknown, icon_themes[i]));
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

        return rootView;
    }

    @Override
    public com.rey.material.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), ThemeHelper.getDialogRes(getActivity()));
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setTitle(R.string.icon_theme);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = radioGroupTheme.getCheckedRadioButtonIndex();
                if (index != -1) {
                    String[] icon_themes = getResources().getStringArray(R.array.default_fallback_icon_set_keys);
                    LoadStoreIconData.setDefaultFallbackIconSet(icon_themes[index]);
                }
                dismiss();
            }
        }).positiveAction(android.R.string.ok);
        return dialog;
    }
}
