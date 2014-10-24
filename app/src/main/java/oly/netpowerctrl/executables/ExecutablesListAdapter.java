package oly.netpowerctrl.executables;

import android.util.SparseBooleanArray;
import android.widget.CheckedTextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.IconDeferredLoadingThread;

public class ExecutablesListAdapter extends ExecutablesBaseAdapter {
    private final SparseBooleanArray checked = new SparseBooleanArray();
    private final boolean checkable;

    public ExecutablesListAdapter(boolean checkable, ExecutablesSourceBase source,
                                  IconDeferredLoadingThread iconCache, boolean showGroups) {
        super(source, iconCache, showGroups);
        this.checkable = checkable;
        if (checkable)
            setLayoutRes(R.layout.list_item_selectable_outlet);
        else
            setLayoutRes(R.layout.list_item_available_outlet);
        if (source != null)
            source.updateNow();
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        super.onBindViewHolder(executableViewHolder, position);

        ExecutableAdapterItem item = mItems.get(position);
        Executable port = item.getExecutable();

        // Not our business, if port is null
        if (port == null)
            return;

        if (checkable) {
            CheckedTextView t = (CheckedTextView) executableViewHolder.title;
            t.setChecked(checked.get(position));
        }
    }

    public void toggleItemChecked(int position) {
        checked.put(position, !checked.get(position));
        notifyDataSetChanged();
    }

//    public List<UUID> getCheckedUUids() {
//        List<UUID> slaves = new ArrayList<>();
//        for (int i = 0; i < getItemCount(); i++) {
//            if (checked.get(i)) {
//                slaves.add(getDevicePort(i).uuid);
//            }
//        }
//        return slaves;
//    }
//
//    public void setChecked(List<UUID> slaves) {
//        for (UUID slave : slaves) {
//            int position = findPositionByUUid(slave);
//            if (position == -1)
//                continue;
//            checked.put(position, true);
//        }
//    }
}
