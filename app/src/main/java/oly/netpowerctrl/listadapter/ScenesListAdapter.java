package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.network.UDPSendToDevice;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.GreenFlasher;
import oly.netpowerctrl.utils.MenuConfigureEvent;

public class ScenesListAdapter extends BaseAdapter implements OnClickListener {
    private Context context;
    private MenuConfigureEvent menuConfigureEvent = null;
    private ArrayList<OutletCommandGroup> scenes;
    private LayoutInflater inflater;

    public ScenesListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        scenes = SharedPrefs.ReadGroups(context);
    }

    @Override
    public int getCount() {
        return scenes.size();
    }

    @Override
    public Object getItem(int position) {
        return scenes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        GreenFlasher.flashBgColor(v);
        executeScene((Integer) v.getTag());
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.scene_list_item, null);
        }

        OutletCommandGroup data = scenes.get(position);

        TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
        tvName.setText(data.groupname);
        tvName.setTag(position);
        tvName.setOnClickListener(this);
        tvName = (TextView) convertView.findViewById(R.id.group_list_details);
        tvName.setText(data.groupdetails);
        tvName.setTag(position);
        tvName.setOnClickListener(this);
        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditScene);
        btn.setTag(position);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (menuConfigureEvent != null) {
                    int position = (Integer) view.getTag();
                    menuConfigureEvent.onConfigure(view, position);
                }
            }
        });

        return convertView;
    }

    public void executeScene(int position) {
        OutletCommandGroup og = (OutletCommandGroup) getItem(position);
        UDPSendToDevice.sendOutlet(context, og);
    }

    public void addScene(OutletCommandGroup data) {
        if (data == null)
            return;

        scenes.add(data);
        SharedPrefs.SaveGroups(scenes, context);
        notifyDataSetChanged();
    }

    public void removeScene(int position) {
        scenes.remove(position);
        SharedPrefs.SaveGroups(scenes, context);
        notifyDataSetChanged();
    }

    public void deleteAll() {
        scenes.clear();
        SharedPrefs.SaveGroups(scenes, context);
        notifyDataSetChanged();
    }

    public void setMenuConfigureEvent(MenuConfigureEvent dce) {
        menuConfigureEvent = dce;
    }
}
