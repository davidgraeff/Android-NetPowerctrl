package oly.netpowerctrl.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.adapter.AdapterInput;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutablesAdapter;

/**
* Created by david on 15.01.15.
*/
public class SelectFromListFragment extends Fragment implements RecyclerItemClickListener.OnItemClickListener {
    private final onItemClicked onItemClicked;
    private RecyclerView.Adapter<?> adapter;
    private AdapterSource adapterSource;
    private AdapterInput[] inputs;

    public SelectFromListFragment() {
        onItemClicked = null;
    }

    /**
     * You have to provide an onClick listener and all the input sources for the list.
     *
     * @param onItemClicked
     * @param inputs
     */
    @SuppressLint("ValidFragment")
    public SelectFromListFragment(onItemClicked onItemClicked, AdapterInput... inputs) {
        this.onItemClicked = onItemClicked;
        this.inputs = inputs;
    }


    public AdapterSource getAdapterSource() {
        return adapterSource;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.setShowHeaders(false);
        adapterSource.addInput(inputs);
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.list_item_available_outlet);

        View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);
        RecyclerViewWithAdapter<?> recyclerViewWithAdapter =
                new RecyclerViewWithAdapter<>(getActivity(), rootView, adapter, 0);
        recyclerViewWithAdapter.setOnItemClickListener(new RecyclerItemClickListener(getActivity(), this, null));
        return rootView;
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        String uid = adapterSource.getItem(position).getExecutableUid();
        if (uid != null)
            onItemClicked.onExecutableSelected(uid, position);
        uid = adapterSource.getItem(position).groupUID();
        if (uid != null)
            onItemClicked.onGroupSelected(uid, position);

        return true;
    }

    public interface onItemClicked {
        void onExecutableSelected(String uid, int position);

        void onGroupSelected(String uid, int position);
    }
}
