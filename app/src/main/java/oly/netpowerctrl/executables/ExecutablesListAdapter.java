package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.executables.Executable;

public class ExecutablesListAdapter extends ExecutablesBaseAdapter {
    private final SparseBooleanArray checked = new SparseBooleanArray();
    private final boolean checkable;

    public ExecutablesListAdapter(boolean checkable, @NonNull ExecutablesSourceBase source,
                                  @NonNull IconDeferredLoadingThread iconCache, boolean showGroups) {
        super(source, iconCache, showGroups);
        this.checkable = checkable;
        if (checkable)
            setLayoutRes(R.layout.list_item_selectable_outlet);
        else
            setLayoutRes(R.layout.list_item_available_outlet);
        source.updateNow();
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

    public List<Boolean> getChecked() {
        List<Boolean> slaves = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            slaves.add(checked.get(i));
        }
        return slaves;
    }

    public void setChecked(SparseBooleanArray checked) {
        for (int i = 0; i < checked.size(); ++i)
            this.checked.put(checked.keyAt(i), checked.valueAt(i));
    }
}
