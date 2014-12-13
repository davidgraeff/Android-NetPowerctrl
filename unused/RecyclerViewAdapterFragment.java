package oly.netpowerctrl.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

/**
 * A fragment with a recyclerview that uses the given adapter type
 */
public class RecyclerViewAdapterFragment<ADAPTER extends RecyclerView.Adapter> extends Fragment {
    protected ADAPTER mAdapter;
    protected RecyclerView mRecyclerView;
    protected View mEmptyView;
    protected int emptyRessource;
    protected int fragmentRes = R.layout.fragment_with_list;
    protected RecyclerItemClickListener onItemClickListener;
    private boolean initDone = false;

    public RecyclerViewAdapterFragment() {
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(fragmentRes, container, false);
        assert view != null;
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if (onItemClickListener != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);

        if (mAdapter != null)
            setAdapter(mAdapter, emptyRessource);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mAdapter != null && mRecyclerView != null && !initDone) {
            initDone = true;
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    public ADAPTER getAdapter() {
        return mAdapter;
    }

    public void setAdapter(ADAPTER adapter, int emptyRessource) {
        this.emptyRessource = emptyRessource;
        mAdapter = adapter;
        if (mAdapter != null && mRecyclerView != null && !initDone) {
            initDone = true;
            mRecyclerView.setAdapter(mAdapter);
            if (emptyRessource != 0) {
                ((TextView)mEmptyView.findViewById(R.id.empty_text)).setText(emptyRessource);
                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        if (mAdapter.getItemCount() == 0) {
                            mEmptyView.setVisibility(View.VISIBLE);
                        } else {
                            mEmptyView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onItemRangeChanged(int positionStart, int itemCount) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeRemoved(int positionStart, int itemCount) {
                        onChanged();
                    }
                });
            }
        }
    }

    public void setOnItemClickListener(RecyclerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        if (mRecyclerView != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
    }
}
