package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dragdrop.DragDropEnabled;
import oly.netpowerctrl.dragdrop.DropListener;
import oly.netpowerctrl.dragdrop.RemoveListener;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ListItemMenu;

public class ScenesListAdapter extends BaseAdapter implements DragDropEnabled, RemoveListener, DropListener {
    private ListItemMenu listItemMenu = null;
    private List<Scene> scenes;
    private LayoutInflater inflater;
    private boolean dragDropEnabled = false;

    public ScenesListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        scenes = SharedPrefs.ReadScenes();
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

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.scene_list_item, null);
        }

        Scene data = scenes.get(position);

        TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
        tvName.setText(data.sceneName);
        tvName = (TextView) convertView.findViewById(R.id.group_list_details);
        tvName.setText(data.sceneDetails);
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
        ImageView handlerImage = (ImageView) convertView.findViewById(R.id.MoveHandler);
        handlerImage.setVisibility(dragDropEnabled ? View.VISIBLE : View.GONE);

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
        data.sceneDetails = data.buildDetails();

        int i = scenes.indexOf(data);
        if (i != -1) {
            scenes.set(i, data);
        } else
            scenes.add(data);

        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public void removeScene(int position) {
        scenes.remove(position);
        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public void deleteAll() {
        scenes.clear();
        SharedPrefs.SaveScenes(scenes);
        notifyDataSetChanged();
    }

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    @Override
    public void onDrop(int from, int to) {
        Scene temp = scenes.get(from);
        scenes.remove(from);
        scenes.add(to, temp);
    }

    @Override
    public void onRemove(int position) {
        if (position < 0 || position > scenes.size()) return;
        scenes.remove(position);
        SharedPrefs.SaveScenes(scenes);
    }

    @Override
    public void setDragDropEnabled(boolean d) {
        dragDropEnabled = d;
        notifyDataSetChanged();
    }

    public void saveScenes() {
        SharedPrefs.SaveScenes(scenes);
    }

    @Override
    public boolean isDragDropEnabled() {
        return dragDropEnabled;
    }
}
