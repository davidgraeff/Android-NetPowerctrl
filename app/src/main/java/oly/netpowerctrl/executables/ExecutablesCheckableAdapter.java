package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckedTextView;

import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.executables.Executable;

public class ExecutablesCheckableAdapter extends ExecutablesAdapter implements View.OnClickListener {
    private Set<String> checked = null;

    public ExecutablesCheckableAdapter(@NonNull AdapterSource source,
                                       @NonNull IconDeferredLoadingThread iconCache) {
        super(source, iconCache, R.layout.list_item_selectable_outlet);
    }

    @Override
    public void onBindViewHolder(final ExecutableViewHolder executableViewHolder, int position) {
        super.onBindViewHolder(executableViewHolder, position);

        ExecutableAdapterItem item = mSource.mItems.get(position);
        Executable port = item.getExecutable();

        // Not our business, if port is null
        if (port == null || checked == null)
            return;

        CheckedTextView t = (CheckedTextView) executableViewHolder.title;
        t.setChecked(checked.contains(port.getUid()));
        t.setTag(position);
        t.setOnClickListener(this);
    }

    public Set<String> getCheckedItems() {
        return checked;
    }

    public void setChecked(@NonNull Set<String> checked) {
        this.checked = checked;
    }

    @Override
    public void onClick(View view) {
        CheckedTextView t = (CheckedTextView) view;
        t.setChecked(!t.isChecked());
        String executableUid = mSource.mItems.get((Integer) t.getTag()).getExecutable().getUid();
        if (t.isChecked())
            checked.add(executableUid);
        else
            checked.remove(executableUid);
    }
}
