package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Scenes;

public class ScenesListAdapter extends AbstractDynamicGridAdapter {
    private class Item {
        Scene scene;
        Bitmap icon;
        long id;

        Item(Scene scene, Bitmap bitmap, long id) {
            this.scene = scene;
            this.icon = bitmap;
            this.id = id;
        }
    }

    private List<Item> scenes = new ArrayList<Item>();
    private LayoutInflater inflater;
    private long nextId = 0;

    public ScenesListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        List<Scene> list_of_scenes = SharedPrefs.ReadScenes();
        for (Scene scene : list_of_scenes) {
            scenes.add(new Item(scene, Scenes.loadIcon(context, scene), nextId++));
        }
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

        Scene data = scenes.get(position).scene;

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
        tvName.setText(data.sceneName);

        ImageView image = (ImageView) convertView.findViewById(R.id.scene_icon_bitmap);
        Bitmap b = scenes.get(position).icon;
        if (b != null) {
            image.setImageBitmap(b);
        } else {
            image.setImageResource(R.drawable.widgeton);
        }
        return convertView;
    }

    public void executeScene(int position) {
        Scene og = getScene(position);
        DeviceSend.instance().sendOutlets(DeviceCommand.fromOutletCommandGroup(og), true);
    }

    public void addScene(Context context, Scene data) {
        if (data == null)
            return;

        data.updateDeviceAndOutletLinks();

        // scenes.indexOf(--data--)
        int position = -1;
        for (int i = 0; i < scenes.size(); ++i)
            if (scenes.get(i).scene.equals(data)) {
                position = i;
                break;
            }

        // Replace existing item
        if (position != -1) {
            Item item = scenes.get(position);
            item.scene = data;
            item.icon = Scenes.loadIcon(context, data);
        } else { // Add new item
            Item item = new Item(data, Scenes.loadIcon(context, data), nextId++);
            scenes.add(item);
        }

        saveScenes();
        notifyDataSetChanged();
    }

    public void removeScene(int position) {
        if (position < 0 || position > scenes.size()) return;
        scenes.remove(position);
        saveScenes();
        notifyDataSetChanged();
    }

    public void deleteAll() {
        scenes.clear();
        saveScenes();
        notifyDataSetChanged();
    }

    public void saveScenes() {
        List<Scene> list_of_scenes = new ArrayList<Scene>();
        for (Item s : scenes)
            list_of_scenes.add(s.scene);
        SharedPrefs.SaveScenes(list_of_scenes);
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition >= getCount()) {
            return;
        }
        Item temp = scenes.get(originalPosition);
        scenes.remove(originalPosition);
        scenes.add(newPosition, temp);
        notifyDataSetChanged();
    }

    @Override
    public void finishedReordering() {
        saveScenes();
    }

    @Override
    public long getItemId(int position) {
        return scenes.get(position).id;
    }

    public boolean contains(Scene scene) {
        for (Item s : scenes)
            if (s.scene.equals(scene))
                return true;
        return false;
    }

    public Scene getScene(int position) {
        return scenes.get(position).scene;
    }
}
