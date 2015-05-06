package oly.netpowerctrl.executables.adapter;

import android.support.annotation.NonNull;
import android.widget.CompoundButton;

import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.IconDeferredLoadingThread;
import oly.netpowerctrl.executables.Executable;

public class ExecutablesCheckableAdapter extends ExecutablesAdapter implements CompoundButton.OnCheckedChangeListener {
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

        CompoundButton t = (CompoundButton) executableViewHolder.title;
        t.setChecked(checked.contains(port.getUid()));
        t.setTag(position);
        t.setOnCheckedChangeListener(this);
    }

    public Set<String> getCheckedItems() {
        return checked;
    }

    public void setChecked(@NonNull Set<String> checked) {
        this.checked = checked;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        String executableUid = mSource.mItems.get((Integer) compoundButton.getTag()).getExecutable().getUid();
        if (b)
            checked.add(executableUid);
        else
            checked.remove(executableUid);
    }
}
