package oly.netpowerctrl.groups;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 *
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> implements onCollectionUpdated<GroupCollection, Group>, onServiceReady {
    private GroupCollection groupCollection;
    private int selectedItemPosition = -1;
    private int lastSelectedItemPosition = -1;
    private List<Group> items = new ArrayList<>();

    public GroupAdapter() {
        DataService.observersServiceReady.register(this);
    }

    /**
     * Return the group at the given position. The first entry is the "all" entry and will return null.
     *
     * @param position
     * @return
     */
    public Group getGroup(int position) {
        if (position == 0) return null;
        return items.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(layoutInflater.inflate(R.layout.list_item_group, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.textView.setText(items.get(position).name);

        if (lastSelectedItemPosition == position) {
            viewHolder.layout_item.setActivated(false);
            lastSelectedItemPosition = -1;
        }

        if (selectedItemPosition == position) {
            viewHolder.layout_item.setActivated(true);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void onDestroy() {
        items.clear();
        groupCollection.unregisterObserver(this);
    }

    private void resetItems() {
        items.clear();
        items.add(new Group("", App.getAppString(R.string.groups_all)));
        for (Group group : groupCollection.getItems().values()) {
            items.add(group);
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean updated(@NonNull GroupCollection groupCollection, Group group, @NonNull ObserverUpdateActions action) {
        switch (action) {
            case AddAction:
                items.add(group);
                notifyItemInserted(items.size() - 1);
                break;
            case RemoveAction:
                for (int i = 0; i < items.size(); ++i)
                    if (items.get(i).getUid().equals(group.getUid())) {
                        items.remove(i);
                        notifyItemRemoved(i);
                        break;
                    }
                break;
            case ClearAndNewAction:
            case RemoveAllAction:
                resetItems();
                break;
            case UpdateAction:
                for (int i = 0; i < items.size(); ++i)
                    if (items.get(i).getUid().equals(group.getUid())) {
                        notifyItemChanged(i);
                        break;
                    }
        }
        return true;
    }

    @Override
    public boolean onServiceReady(DataService service) {
        groupCollection = service.groups;
        groupCollection.registerObserver(this);
        resetItems();
        return true;
    }

    @Override
    public void onServiceFinished(DataService service) {
        groupCollection = null;
    }

    public void setSelectedItem(String groupUID) {
        lastSelectedItemPosition = selectedItemPosition;
        if (lastSelectedItemPosition != -1)
            notifyItemChanged(lastSelectedItemPosition);

        int pos = -1;
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).getUid().equals(groupUID)) {
                pos = i;
                break;
            }

        selectedItemPosition = pos;
        if (pos == -1) return;
        notifyItemChanged(pos);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        View layout_item;

        public ViewHolder(View itemView) {
            super(itemView);
            layout_item = itemView.findViewById(R.id.list_item);
            textView = (TextView) itemView.findViewById(R.id.title);
        }
    }
}
