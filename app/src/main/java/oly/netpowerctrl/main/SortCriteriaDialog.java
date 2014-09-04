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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.SortCriteriaInterface;

/**
 * Choose setSortOrder criteria
 */
public class SortCriteriaDialog extends DialogFragment {
    private final List<AdapterItem> sortedList = new ArrayList<>();
    private final List<AdapterItem> availableList = new ArrayList<>();
    private SortCriteriaInterface sortCriteriaInterface;
    private boolean criteriaChecked[];
    private boolean criteriaOnly = true;
    private ArrayAdapter<AdapterItem> sortedAdapter;
    private ArrayAdapter<AdapterItem> availableAdapter;

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
                .setTitle(R.string.menu_sort)
                .setView(createView(inflater, null, savedInstanceState))
                .setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (criteriaOnly) {
                            sortCriteriaInterface.applySortCriteria(criteriaChecked);
                        } else {
                            int sortOrder[] = new int[sortedList.size()];
                            for (int i = 0; i < sortOrder.length; ++i)
                                sortOrder[i] = sortedList.get(i).originalIndex;
                            sortCriteriaInterface.setSortOrder(sortOrder);
                        }
                    }
                })
                .setNeutralButton(R.string.menu_help, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.menu_help)
                                .setMessage(R.string.help_sorting)
                                .setIcon(android.R.drawable.ic_menu_help).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        return builder.create();
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

        /// Custom sort ///
        CheckBox customCheck = (CheckBox) view.findViewById(R.id.activate_custom_sort);
        final View customContainer = view.findViewById(R.id.customSortContainer);
        customContainer.setVisibility(
                sortCriteriaInterface.allowCustomSort() ? View.VISIBLE : View.GONE);
        customCheck.setVisibility(
                sortCriteriaInterface.allowCustomSort() ? View.VISIBLE : View.GONE);
        customCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                customContainer.setVisibility(b ? View.VISIBLE : View.GONE);
                criteriaOnly = !b;
                AlertDialog d = (AlertDialog) getDialog();
                if (!b) {
                    d.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                } else
                    //noinspection ConstantConditions
                    d.getButton(Dialog.BUTTON_POSITIVE).setEnabled(availableAdapter.isEmpty());
            }
        });
        customContainer.setVisibility(View.GONE);

        /// Custom sort Adapter ///
        String[] data = sortCriteriaInterface.getContentList(0);
        int index = 0;
        for (String d : data)
            availableList.add(new AdapterItem(d, index++));

        availableAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                availableList);

        sortedAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                sortedList);

        /// Custom sort lists ///
        ListView listSorted = (ListView) view.findViewById(R.id.list_sorted);
        listSorted.setAdapter(sortedAdapter);
        listSorted.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                swapEntry(sortedAdapter, availableAdapter, i);
            }
        });
        ListView listAvailable = (ListView) view.findViewById(R.id.list_available);
        listAvailable.setAdapter(availableAdapter);
        listAvailable.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                swapEntry(availableAdapter, sortedAdapter, i);
            }
        });
        return view;
    }

    private void swapEntry(ArrayAdapter<AdapterItem> a, ArrayAdapter<AdapterItem> b, int posFromA) {
        AdapterItem item = a.getItem(posFromA);
        a.remove(item);
        b.add(item);
        AlertDialog d = (AlertDialog) getDialog();
        //noinspection ConstantConditions
        d.getButton(Dialog.BUTTON_POSITIVE).setEnabled(availableAdapter.isEmpty());
    }

    private static class AdapterItem {
        final String text;
        final int originalIndex;

        private AdapterItem(String text, int originalIndex) {
            this.text = text;
            this.originalIndex = originalIndex;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
