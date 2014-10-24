package oly.netpowerctrl.executables;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.RecyclerItemClickListener;

/**
 * Created by david on 06.09.14.
 */
public class AdapterFragment<ADAPTER extends RecyclerView.Adapter> extends Fragment {
    protected ADAPTER mAdapter;
    protected RecyclerView mRecyclerView;
    protected RecyclerItemClickListener onItemClickListener;
    private boolean initDone = false;

    public AdapterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_with_list, container, false);
        assert view != null;
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        if (onItemClickListener != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);

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

    public void setAdapter(ADAPTER adapter) {
        mAdapter = adapter;
        if (mAdapter != null && mRecyclerView != null && !initDone) {
            initDone = true;
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    public void setOnItemClickListener(RecyclerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        if (mRecyclerView != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
    }
}
