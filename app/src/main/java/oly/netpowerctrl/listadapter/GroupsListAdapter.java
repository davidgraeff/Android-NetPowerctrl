package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.Groups;
import oly.netpowerctrl.utils.Icons;

public class GroupsListAdapter extends BaseAdapter implements Groups.IGroupsUpdated {
    private LayoutInflater inflater;
    Groups groups;

    public GroupsListAdapter(Context context, Groups data) {
        inflater = LayoutInflater.from(context);
        groups = data;
        groups.registerObserver(this);
    }

    @Override
    public int getCount() {
        return groups.length();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return groups.groupItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return groups.groupItems.get(position).id;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.scene_list_item, parent);
        }

        Groups.GroupItem data = groups.groupItems.get(position);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(R.id.scene_list_name);
        tvName.setText(data.name);

        ImageView image = (ImageView) convertView.findViewById(R.id.scene_icon_bitmap);
        if (data.bitmap == null) {
            data.bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, data.uuid,
                    Icons.IconType.SceneIcon, R.drawable.stateon);
        }

        image.setImageBitmap(data.bitmap);
        return convertView;
    }

    @Override
    public void groupsUpdated(boolean addedOrRemoved) {
        notifyDataSetChanged();
    }
}
