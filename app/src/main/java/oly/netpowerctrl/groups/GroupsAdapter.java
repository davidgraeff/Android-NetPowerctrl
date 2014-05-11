package oly.netpowerctrl.groups;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;

public class GroupsAdapter extends BaseAdapter implements GroupCollection.IGroupsUpdated {
    private final LayoutInflater inflater;
    private final GroupCollection groupCollection;

    public GroupsAdapter(Context context, GroupCollection data) {
        inflater = LayoutInflater.from(context);
        groupCollection = data;
        groupCollection.registerObserver(this);
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
        return groupCollection.groups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return groupCollection.groups.get(position).id;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_icon_item, parent);
        }

        GroupCollection.GroupItem data = groupCollection.groups.get(position);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(R.id.text1);
        tvName.setText(data.name);

        ImageView image = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        if (data.bitmap == null) {
            data.bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, data.uuid,
                    Icons.IconType.GroupIcon, Icons.IconState.StateUnknown, R.drawable.stateon);
        }

        image.setImageBitmap(data.bitmap);
        return convertView;
    }

    @Override
    public void groupsUpdated(boolean addedOrRemoved) {
        notifyDataSetChanged();
    }
}
