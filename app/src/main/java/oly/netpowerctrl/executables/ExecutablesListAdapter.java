package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckedTextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.executables.Executable;

public class ExecutablesListAdapter extends ExecutablesBaseAdapter {
    private final boolean checkable;
    private SparseBooleanArray checked = new SparseBooleanArray();

    public ExecutablesListAdapter(boolean checkable, @NonNull AdapterSource source,
                                  @NonNull IconDeferredLoadingThread iconCache, boolean showGroups) {
        super(source, iconCache, showGroups);
        this.checkable = checkable;
        if (checkable)
            setLayoutRes(R.layout.list_item_selectable_outlet);
        else
            setLayoutRes(R.layout.list_item_available_outlet);
        source.setTargetAdapter(this);
    }

    @Override
    public void onBindViewHolder(final ExecutableViewHolder executableViewHolder, final int position) {
        super.onBindViewHolder(executableViewHolder, position);

        ExecutableAdapterItem item = mItems.get(position);
        Executable port = item.getExecutable();

        // Not our business, if port is null
        if (port == null)
            return;

        if (checkable) {
            final CheckedTextView t = (CheckedTextView) executableViewHolder.title;
            t.setChecked(checked.get(position));
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    t.setChecked(!t.isChecked());
                    checked.put(position, t.isChecked());
                }
            });
        }
    }

    public void toggleItemChecked(int position) {
        checked.put(position, !checked.get(position));
        notifyDataSetChanged();
    }

    public Boolean[] getChecked() {
        Boolean[] slaves = new Boolean[getItemCount()];
        for (int i = 0; i < slaves.length; i++) {
            slaves[i] = checked.get(i);
        }
        return slaves;
    }

    public void setChecked(Boolean[] checked) {
        for (int i = 0; i < checked.length; ++i)
            this.checked.put(i, checked[i]);
    }
}
