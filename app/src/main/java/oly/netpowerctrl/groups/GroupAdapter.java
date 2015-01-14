package oly.netpowerctrl.groups;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;

/**
 *
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> implements onCollectionUpdated<GroupCollection, Group>, onServiceReady {
    private int count;
    private GroupCollection groupCollection;
    private int selectedItemPosition = -1;
    private int lastSelectedItemPosition = -1;

    public GroupAdapter() {
        count = 0;
        PluginService.observersServiceReady.register(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(layoutInflater.inflate(R.layout.list_item_group, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        String t = position == 0 ? App.getAppString(R.string.groups_all) :
                groupCollection.get(position - 1).name;
        viewHolder.textView.setText(t);

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
        return count;
    }

    public void onDestroy() {
        groupCollection.unregisterObserver(this);
    }

    @Override
    public boolean updated(@NonNull GroupCollection groupCollection, Group group, @NonNull ObserverUpdateActions action, int position) {
        count = groupCollection.size() + 1;
        notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        groupCollection = service.getAppData().groupCollection;
        groupCollection.registerObserver(GroupAdapter.this);
        count = groupCollection.size() + 1;
        notifyDataSetChanged();
        return true;
    }

    @Override
    public void onServiceFinished(PluginService service) {
        groupCollection = null;
    }

    public void setSelectedItem(int pos) {
        lastSelectedItemPosition = selectedItemPosition;
        if (lastSelectedItemPosition != -1)
            notifyItemChanged(lastSelectedItemPosition);

        selectedItemPosition = pos;
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
