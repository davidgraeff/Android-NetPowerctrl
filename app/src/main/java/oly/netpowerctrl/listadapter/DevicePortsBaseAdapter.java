package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsBaseAdapter extends AbstractDynamicGridAdapter {

    protected int nextId = 0; // we need stable IDs for the gridView
    protected int outlet_res_id = R.layout.list_icon_item;
    protected boolean showHidden = true;
    protected ViewHolder current_viewHolder;
    private IconDeferredLoadingThread iconCache = new IconDeferredLoadingThread();

    // Some observers
    private ListItemMenu mListContextMenu = null;
    protected ListItemMenu mListItemMenu = null;

    //ViewHolder pattern
    protected static class ViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
        ImageView imageIcon;
        ImageView imageEdit;
        //LinearLayout mainTextView;
        View entry;
        SeekBar seekBar;
        TextView title;
        TextView subtitle;
        boolean isNew = true;

        int currentBitmapIndex = 0;
        Drawable[] drawables = new Drawable[2];
        public int position;
        private ListItemMenu mListContextMenu = null;
        private IconDeferredLoadingThread iconCache;

        ViewHolder(View convertView, ListItemMenu listContextMenu, IconDeferredLoadingThread iconCache) {
            mListContextMenu = listContextMenu;
            this.iconCache = iconCache;
            imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
            imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);
            seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
            //mainTextView = (LinearLayout) convertView.findViewById(R.id.outlet_list_text);
            entry = convertView.findViewById(R.id.item_layout);
            title = (TextView) convertView.findViewById(R.id.title);
            subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        }

        public void loadIcon(UUID uuid, Icons.IconType iconType, Icons.IconState state, int default_resource, int bitmapPosition) {
            iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                    uuid, iconType, state, default_resource, this, bitmapPosition));
        }

        public void setCurrentBitmapIndex(int index) {
            currentBitmapIndex = index;
            if (drawables[index] != null)
                imageIcon.setImageDrawable(drawables[index]);
        }

        @Override
        public void onClick(View view) {
            mListContextMenu.onMenuItemClicked(view, position);
        }

        @Override
        public void setDrawable(Drawable bitmap, int position) {
            drawables[position] = bitmap;
            if (currentBitmapIndex == position)
                imageIcon.setImageDrawable(drawables[position]);
        }
    }

    protected static class DevicePortListItem {
        public DevicePort port;
        String displayText;
        // unique id for the gridView
        public long id;
        // If you change a DevicePort's value, that new value may be stored in
        // command_value instead overwriting DevicePort's value. The implementation
        // depends on the child class.
        public int command_value;

        DevicePortListItem(DevicePort oi, boolean showDevice, int command_value, long id) {
            this.id = id;
            this.port = oi;
            this.command_value = command_value;
            if (showDevice)
                displayText = oi.device.DeviceName + ": " + oi.getDescription();
            else
                displayText = oi.getDescription();
        }

        public boolean isEnabled() {
            return port.last_command_timecode <= port.device.getUpdatedTime();
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    protected List<DevicePortListItem> all_outlets;
    protected LayoutInflater inflater;
    protected boolean temporary_ignore_positionRequest;
    protected Context context;
    private UUID filterGroup = null;

    protected void setGroupFilter(UUID groupFilter) {
        this.filterGroup = groupFilter;
    }

    public int getLayoutRes() {
        return outlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.outlet_res_id = layout_res;
        notifyDataSetChanged();
    }

    protected DevicePortsBaseAdapter(Context context, ListItemMenu mListContextMenu, UUID filterGroup) {
        this.mListContextMenu = mListContextMenu;
        this.context = context;
        this.filterGroup = filterGroup;
        inflater = LayoutInflater.from(context);
        iconCache.start();
        all_outlets = new ArrayList<DevicePortListItem>();
    }

    /**
     * Call this to load AnelDeviceSwitch data from a scene.
     *
     * @param scene
     */
    public void loadByScene(Scene scene) {
        for (Scene.SceneItem sceneItem : scene.sceneItems) {
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(sceneItem.uuid);
            if (port == null) {
                continue;
            }
            addItem(port, sceneItem.command);
        }
        notifyDataSetChanged();
    }

    public List<Scene.SceneItem> getScene() {
        List<Scene.SceneItem> list_of_scene_items = new ArrayList<Scene.SceneItem>();
        for (DevicePortListItem info : all_outlets) {
            list_of_scene_items.add(new Scene.SceneItem(info.port.uuid, info.command_value));
        }
        return list_of_scene_items;
    }

    public void setListItemMenu(ListItemMenu listItemMenu) {
        this.mListItemMenu = listItemMenu;
    }

    @Override
    public int getCount() {
        return all_outlets.size();
    }

    public DevicePort getItem(int position) {
        return all_outlets.get(position).port;
    }

    @Override
    public long getItemId(int position) {
        return all_outlets.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            current_viewHolder = new ViewHolder(convertView, mListContextMenu, iconCache);
            convertView.setTag(current_viewHolder);
        } else {
            current_viewHolder = (ViewHolder) convertView.getTag();
            current_viewHolder.isNew = false;
        }
        current_viewHolder.position = position;

        DevicePortListItem info = all_outlets.get(position);

        current_viewHolder.entry.setAlpha(info.port.Hidden ? 0.5f : 1.0f);
        current_viewHolder.entry.setEnabled(info.isEnabled());

        current_viewHolder.title.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_viewHolder.title.setText(info.port.getDescription());
        current_viewHolder.title.setEnabled(info.isEnabled());

        current_viewHolder.subtitle.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_viewHolder.subtitle.setText(info.port.device.DeviceName);
        current_viewHolder.subtitle.setEnabled(info.isEnabled());

        return convertView;
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition >= getCount()) {
            return;
        }
        DevicePortListItem temp = all_outlets.get(originalPosition);
        all_outlets.get(originalPosition).port.positionRequest = all_outlets.get(newPosition).port.positionRequest;
        all_outlets.get(newPosition).port.positionRequest = temp.port.positionRequest;
        all_outlets.remove(originalPosition);
        all_outlets.add(newPosition, temp);
        notifyDataSetChanged();
    }

    @Override
    public void finishedReordering() {
        NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
    }

    public void addItem(DevicePort oi, int command_value) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

        // FilterGroup
        if (filterGroup != null) {
            if (!oi.groups.contains(filterGroup))
                return;
        }

        DevicePortListItem new_oi = new DevicePortListItem(oi, true, command_value, nextId++);

        boolean found = false;
        for (int i = 0; i < all_outlets.size(); ++i) {
            boolean behind_current = temporary_ignore_positionRequest ?
                    all_outlets.get(i).displayText.compareTo(new_oi.displayText) > 0 :
                    all_outlets.get(i).port.positionRequest > new_oi.port.positionRequest;

            if (behind_current) {
                all_outlets.add(i, new_oi);
                found = true;
                break;
            }
        }
        if (!found)
            all_outlets.add(new_oi);
    }

    public List<DeviceInfo> update(List<DeviceInfo> all_devices) {
        // Clear
        all_outlets.clear();

        List<DeviceInfo> not_reachable = new ArrayList<DeviceInfo>();
        for (DeviceInfo device : all_devices) {
            if (!device.isReachable()) {
                not_reachable.add(device);
                continue;
            }

            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                DevicePort oi = it.next();
                addItem(oi, oi.current_value);
            }
            device.releaseDevicePorts();
        }

        // Sort for positionRequest number or alphabetically
        //Collections.sort(all_outlets);

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(i).port.positionRequest = i;
        }
        notifyDataSetChanged();
        return not_reachable;
    }

    public void clear() {
        all_outlets.clear();
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        all_outlets.remove(position);
        notifyDataSetChanged();
    }
}
