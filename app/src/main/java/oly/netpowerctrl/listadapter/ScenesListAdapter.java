package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.GreenFlasher;
import oly.netpowerctrl.utils.ListItemMenu;

public class ScenesListAdapter extends BaseAdapter implements OnClickListener {
    private Context context;
    private ListItemMenu listItemMenu = null;
    private List<Scene> scenes;
    private LayoutInflater inflater;

    public ScenesListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        scenes = SharedPrefs.ReadScenes(context);
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

        Scene data = scenes.get(position);

        TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
        tvName.setText(data.sceneName);
        tvName.setTag(position);
        tvName.setOnClickListener(this);
        tvName = (TextView) convertView.findViewById(R.id.group_list_details);
        tvName.setText(data.sceneDetails);
        tvName.setTag(position);
        tvName.setOnClickListener(this);
        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditScene);
        btn.setTag(position);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listItemMenu != null) {
                    int position = (Integer) view.getTag();
                    listItemMenu.onMenuItemClicked(view, position);
                }
            }
        });

        return convertView;
    }

    void executeScene(int position) {
        Scene og = (Scene) getItem(position);
        DeviceSend.sendOutlet(context, DeviceCommand.fromOutletCommandGroup(og), true);
    }

    public void addScene(Scene data) {
        if (data == null)
            return;

        data.updateDeviceAndOutletLinks();

        int i = scenes.indexOf(data);
        if (i != -1) {
            scenes.set(i, data);
        } else
            scenes.add(data);

        SharedPrefs.SaveScenes(scenes, context);
        notifyDataSetChanged();
    }

    public void removeScene(int position) {
        scenes.remove(position);
        SharedPrefs.SaveScenes(scenes, context);
        notifyDataSetChanged();
    }

    public void deleteAll() {
        scenes.clear();
        SharedPrefs.SaveScenes(scenes, context);
        notifyDataSetChanged();
    }

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    public List<Scene> getScenes() {
        return scenes;
    }
}
