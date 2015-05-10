package oly.netpowerctrl.scenes;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rey.material.app.Dialog;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.adapter.ExecutablesCheckableAdapter;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.SimpleListDividerDecoration;

/**
 * Presents a list of all DevicePorts/Items of a device each with a checkbox to show/hide the item.
 */
public class SceneElementsAddDialog extends DialogFragment implements RecyclerItemClickListener.OnItemClickListener {
    private RecyclerItemClickListener onItemClickListener;
    private ExecutablesCheckableAdapter adapter;
    private SceneElementsAssigning sceneElementsAssigning;

    public SceneElementsAddDialog() {
    }

    public void setSceneElementsAssigning(SceneElementsAssigning sceneElementsAssigning) {
        this.sceneElementsAssigning = sceneElementsAssigning;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (sceneElementsAssigning == null) {
            return new View(container.getContext());
        }

        final View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
        swipeRefreshLayout.setEnabled(false);
        // Create list view
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new SimpleListDividerDecoration(ContextCompat.getDrawable(getActivity(), R.drawable.list_divider), true));
        // Item click listener
        onItemClickListener = new RecyclerItemClickListener(getActivity(), this, null);
        mRecyclerView.addOnItemTouchListener(onItemClickListener);
        // Adapter (Checkable list) and Adapter Source (DevicePorts of one Device)
        adapter = new ExecutablesCheckableAdapter(sceneElementsAssigning.getAdaperSourceAvailable(), LoadStoreIconData.iconLoadingThread);
        mRecyclerView.setAdapter(adapter);
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setTitle(R.string.scene_available);
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sceneElementsAssigning.addToScene(adapter.getCheckedItems());
                dismiss();
            }
        }).positiveAction(android.R.string.ok);
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        }).negativeAction(android.R.string.cancel);
        return dialog;
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        return false;
    }
}
