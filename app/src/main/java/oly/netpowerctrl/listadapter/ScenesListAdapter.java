package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridStableIDAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ListItemMenu;

public class ScenesListAdapter extends AbstractDynamicGridStableIDAdapter {
    private ListItemMenu listItemMenu = null;
    private List<Scene> scenes;
    private LayoutInflater inflater;

    public ScenesListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        scenes = SharedPrefs.ReadScenes();
        addAllStableId(scenes);
    }

    @Override
    public int getCount() {
        return scenes.size();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return scenes.get(position);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.scene_list_item, null);
        }

        Scene data = scenes.get(position);

        TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
        tvName.setText(data.sceneName);
        //ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditScene);
        //btn.setImageBitmap();
        return convertView;
    }

    public void executeScene(int position) {
        Scene og = (Scene) getItem(position);
        DeviceSend.instance().sendOutlets(DeviceCommand.fromOutletCommandGroup(og), true);
    }

    public void addScene(Scene data) {
        if (data == null)
            return;

        data.updateDeviceAndOutletLinks();

        int position = scenes.indexOf(data);
        if (position != -1) {
            removeStableID(scenes.get(position));
            scenes.set(position, data);
            addStableId(data);
        } else {
            scenes.add(data);
            addStableId(data);
        }

        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public void removeScene(int position) {
        if (position < 0 || position > scenes.size()) return;
        removeStableID(scenes.get(position));
        scenes.remove(position);
        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public void deleteAll() {
        scenes.clear();
        clearStableIdMap();
        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    public void saveScenes() {
        SharedPrefs.SaveScenes(scenes);
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition >= getCount()) {
            return;
        }
        Scene temp = scenes.get(originalPosition);
        scenes.remove(originalPosition);
        scenes.add(newPosition, temp);
        notifyDataSetChanged();
    }

    @Override
    public void finishedReordering() {
        saveScenes();
    }
}
