package oly.netpowerctrl.groups;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;

public class GroupsAdapter extends BaseAdapter implements onCollectionUpdated<GroupCollection, Group> {
    private final LayoutInflater inflater;
    private final GroupCollection groupCollection;
    private final Context context;

    public GroupsAdapter(Context context, GroupCollection data) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        groupCollection = data;
        groupCollection.registerObserver(this);
    }

    public void finish() {
        groupCollection.unregisterObserver(this);
    }

    @Override
    public int getCount() {
        return groupCollection.length();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return groupCollection.get(position);
    }

    @Override
    public long getItemId(int position) {
        return groupCollection.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item_icon, parent);
        }

        Group data = groupCollection.get(position);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(R.id.text1);
        tvName.setText(data.name);

        ImageView image = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        if (data.bitmap == null) {
            data.bitmap = LoadStoreIconData.loadIcon(context, data.uuid,
                    LoadStoreIconData.IconType.GroupIcon, LoadStoreIconData.IconState.StateUnknown, R.drawable.stateon);
        }

        image.setImageBitmap(data.bitmap);
        return convertView;
    }

    @Override
    public boolean updated(GroupCollection groupCollection, Group group, ObserverUpdateActions action) {
        notifyDataSetChanged();
        return true;
    }
}
