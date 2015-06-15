package oly.netpowerctrl.executables.adapter;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.IconDeferredLoadingThread;
import oly.netpowerctrl.executables.Executable;

public class ExecutablesAdapter extends RecyclerView.Adapter<ExecutableViewHolder> {
    protected final AdapterSource mSource;
    private final IconDeferredLoadingThread mIconLoadThread;
    protected int mItemsResourceId = 0;
    private ItemsInRow itemsInRow = new ItemsInRow() {
        @Override
        public int getItemsInRow() {
            return 1;
        }
    };
    GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            if (mSource.mItems.size() <= position) return 1;

            final ExecutableAdapterItem item = mSource.mItems.get(position);
            if (item.getExecutable() == null)
                return itemsInRow.getItemsInRow(); // Only one header per row -> span full row
            else
                return 1;
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
        // viewholder have to be recreated
        notifyItemRangeRemoved(0, mSource.mItems.size());
        notifyItemRangeInserted(0, mSource.mItems.size());
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
            //executableViewHolder.line.setVisibility(mItemsInRow > 1 ? View.VISIBLE : View.GONE);
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

    public void setItemsInRow(ItemsInRow itemsInRow) {
        this.itemsInRow = itemsInRow;
    }

    //////////////// Group Spans //////////////

    public interface ItemsInRow {
        int getItemsInRow();
    }
}
