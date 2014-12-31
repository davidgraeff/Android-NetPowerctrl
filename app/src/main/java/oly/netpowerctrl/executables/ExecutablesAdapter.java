package oly.netpowerctrl.executables;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.executables.Executable;

public class ExecutablesAdapter extends RecyclerView.Adapter<ExecutableViewHolder> {
    protected final AdapterSource mSource;
    private final IconDeferredLoadingThread mIconLoadThread;
    protected int mItemsResourceId = 0;

    private int mItemsInRow = 1;
    GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            final ExecutableAdapterItem item = mSource.mItems.get(position);
            final Executable executable = item.getExecutable();
            if (executable == null)
                return mItemsInRow;
            else
                return 1; // Only one header per row
        }
    };

    public ExecutablesAdapter(AdapterSource source, IconDeferredLoadingThread iconCache,
                              int outlet_res_id) {
        mSource = source;
        mItemsResourceId = outlet_res_id;
        mIconLoadThread = iconCache;
        if (source != null) {
            source.setTargetAdapter(this);
        }
    }


    public AdapterSource getSource() {
        return mSource;
    }

    public void setLayoutRes(int layout_res) {
        this.mItemsResourceId = layout_res;
    }

    @Override
    public int getItemViewType(int position) {
        return mSource.mItems.get(position).getItemViewType() + mItemsResourceId;
    }

    @Override
    public ExecutableViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        viewType -= mItemsResourceId;

        ExecutableAdapterItem.groupTypeEnum groupTypeEnum;
        if (viewType >= 100) {
            groupTypeEnum = ExecutableAdapterItem.groupTypeEnum.values()[viewType - 100];
        } else
            groupTypeEnum = ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE;

        View view = null;

        switch (groupTypeEnum) {
            case NOGROUP_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(mItemsResourceId, viewGroup, false);
                break;
            case GROUP_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_header_icon, viewGroup, false);
                break;
        }

        return new ExecutableViewHolder(view, groupTypeEnum, mIconLoadThread);
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return spanSizeLookup;
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        final ExecutableAdapterItem item = mSource.mItems.get(position);
        final Executable executable = item.getExecutable();

        executableViewHolder.position = position;
        executableViewHolder.setExecutable(executable);
        if (executable == null) { // header
            executableViewHolder.line.setVisibility(mItemsInRow > 1 ? View.VISIBLE : View.GONE);
            executableViewHolder.title.setText(item.groupName);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position >= mSource.mItems.size()) return -1;
        return mSource.mItems.get(position).id;
    }

    @Override
    public int getItemCount() {
        return mSource.mItems.size();
    }

    //////////////// Group Spans //////////////

    public void setItemsInRow(int itemsInRow) {
        if (itemsInRow == this.mItemsInRow)
            return;

        this.mItemsInRow = itemsInRow;

        notifyDataSetChanged();
    }
}
