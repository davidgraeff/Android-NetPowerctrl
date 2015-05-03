package oly.netpowerctrl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;

/**
 * A RecyclerView that has uses the given adapter and supports an empty view.
 */
public class RecyclerViewWithAdapter<ADAPTER extends RecyclerView.Adapter> {
    protected ADAPTER mAdapter;
    protected RecyclerView mRecyclerView;
    protected RecyclerItemClickListener onItemClickListener;
    protected View mEmptyView;
    protected int mEmptyResText;

    private RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
        private void changed() {
            if (mAdapter.getItemCount() != 0 || mEmptyResText == 0) {
                mEmptyView.setVisibility(View.GONE);
                return;
            }

            mEmptyView.setVisibility(View.VISIBLE);
            TextView textView = (TextView) mEmptyView.findViewById(R.id.empty_text);
            textView.setText(mEmptyResText);
        }

        public void onChanged() {
            changed();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            changed();
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            changed();
        }
    };

    /**
     * Create an object of this class in the onCreateView method of your fragment. You can access
     * the recyclerView and adapter by the access methods {@link #getRecyclerView()} and
     * {@link #getAdapter()}. The rootView has to contain a RecyclerView with the id android.R.id.list
     * and an container for the empty text with the id android.R.id.empty. The empty text itself (if used)
     * have to have the id R.id.empty_text.
     *  @param context      A context
     * @param rootView     The root view
     * @param adapter      The adapter
     * @param emptyResText The text resource for the empty text that will be shown if the adapter count is 0.
     */
    public RecyclerViewWithAdapter(@NonNull Context context,
                                   @NonNull View rootView, ADAPTER adapter, int emptyResText) {
        mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mEmptyView = rootView.findViewById(android.R.id.empty);
        mAdapter = adapter;
        if (mRecyclerView == null || mEmptyView == null)
            throw new RuntimeException("RecyclerViewWithAdapter: rootView does not contain list or empty id!");
        if (mAdapter == null)
            throw new RuntimeException("RecyclerViewWithAdapter: adapter not set!");

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        if (onItemClickListener != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.registerAdapterDataObserver(adapterDataObserver);
        this.mEmptyResText = emptyResText;

        adapterDataObserver.onChanged();
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public ADAPTER getAdapter() {
        return mAdapter;
    }

    public void setOnItemClickListener(@NonNull RecyclerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        mRecyclerView.addOnItemTouchListener(onItemClickListener);
    }

    public void onDestroy() {
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);
    }
}
