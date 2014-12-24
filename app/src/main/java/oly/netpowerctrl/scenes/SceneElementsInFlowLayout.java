package oly.netpowerctrl.scenes;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.ui.RecyclerItemClickListener;

/**
 * Synchronize scene elements of a FlowLayout to a SceneElementsAdapter.
 */
public class SceneElementsInFlowLayout extends RecyclerView.AdapterDataObserver {
    private final FlowLayout flowLayout;
    private final SceneElementsAdapter adapter_included;
    private RecyclerItemClickListener.OnItemClickListener onCloseClickListener;
    private List<SceneElementsAdapter.ViewHolder> elements = new ArrayList<>();

    public SceneElementsInFlowLayout(FlowLayout flowLayout,
                                     SceneElementsAdapter adapter_included,
                                     RecyclerItemClickListener.OnItemClickListener onCloseClickListener) {
        this.flowLayout = flowLayout;
        this.adapter_included = adapter_included;
        this.onCloseClickListener = onCloseClickListener;

        adapter_included.registerAdapterDataObserver(this);
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        for (int i = positionStart + itemCount - 1; i >= positionStart; --i) {
            SceneElementsAdapter.ViewHolder vh = elements.get(positionStart);
            elements.remove(i);
            flowLayout.removeView(vh.itemView);
            adapter_included.onViewDetachedFromWindow(vh);
            adapter_included.onViewRecycled(vh);
        }
        for (int i = positionStart; i < adapter_included.getItemCount(); ++i) {
            SceneElementsAdapter.ViewHolder vh = elements.get(i);
            vh.position = i;
        }
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        for (int i = positionStart; i < positionStart + itemCount; ++i) {
            final SceneElementsAdapter.ViewHolder vh = adapter_included.createViewHolder(flowLayout,
                    adapter_included.mItems.get(i).getItemViewType());
            vh.position = i;
            adapter_included.onBindViewHolder(vh, i);
            elements.add(i, vh);
            vh.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCloseClickListener.onItemClick(null, vh.position, false);
                }
            });
            FlowLayout.LayoutParams lp = new FlowLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            flowLayout.addView(vh.itemView, lp);
            adapter_included.onViewAttachedToWindow(vh);
        }
    }
}
