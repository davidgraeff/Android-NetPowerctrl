package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.SortCriteriaInterface;

/**
 * Choose setSortOrder criteria
 */
public class SortCriteriaDialog extends DialogFragment {
    private SortCriteriaInterface sortCriteriaInterface;
    private boolean criteriaChecked[];

    public static DialogFragment instantiate(Context context, SortCriteriaInterface sortCriteriaInterface) {
        SortCriteriaDialog fragment = (SortCriteriaDialog) Fragment.instantiate(context, SortCriteriaDialog.class.getName());
        fragment.setData(sortCriteriaInterface);
        return fragment;
    }

    private void setData(SortCriteriaInterface sortCriteriaInterface) {
        this.sortCriteriaInterface = sortCriteriaInterface;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.sort_title)
                .setView(createView(inflater, null, savedInstanceState))
                .setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sortCriteriaInterface.applySortCriteria(criteriaChecked);
                    }
                })
                .setNeutralButton(R.string.sort_custom, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DialogFragment customDialog = SortCustomDialog.instantiate(getActivity(), sortCriteriaInterface);
                        MainActivity.getNavigationController().changeToDialog(getActivity(), customDialog);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        d.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(
                sortCriteriaInterface.allowCustomSort() ? View.VISIBLE : View.GONE);
    }

    View createView(LayoutInflater inflater, final ViewGroup container,
                    Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_sort_criteria, container, false);
        assert view != null;

        LinearLayout l = (LinearLayout) view.findViewById(R.id.sortCriteriaContainer);

        /// Sort criteria ///
        String[] sortCriteria = sortCriteriaInterface.getSortCriteria();
        criteriaChecked = new boolean[sortCriteria.length];
        int criteriaIndex = 0;
        for (String criteria : sortCriteria) {
            CheckBox c = new CheckBox(view.getContext());
            final int currentIndex = criteriaIndex;
            c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    criteriaChecked[currentIndex] = b;
                }
            });
            c.setText(criteria);
            l.addView(c, criteriaIndex);
            ++criteriaIndex;
        }

        return view;
    }
}
