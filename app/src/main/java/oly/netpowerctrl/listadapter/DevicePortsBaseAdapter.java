package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsBaseAdapter extends BaseAdapter {

    private int nextId = 0; // we need stable IDs for the gridView
    int outlet_res_id = R.layout.list_icon_item;
    // If you change the layout or an image we increment this layout change id
    // to invalidate ViewHolders (for reloading images or layout items).
    int layoutChangeId = 0;
    boolean showHidden = true;
    ViewHolder current_viewHolder;
    private final IconDeferredLoadingThread iconCache = new IconDeferredLoadingThread();

    // Some observers
    private ListItemMenu mListContextMenu = null;
    ListItemMenu mListItemMenu = null;

    //ViewHolder pattern
    protected static class ViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
        final ImageView imageIcon;
        final ImageView imageEdit;
        //LinearLayout mainTextView;
        final View entry;
        final SeekBar seekBar;
        final TextView title;
        final TextView subtitle;
        boolean isNew = true;
        int layoutChangeId;

        int currentBitmapIndex = 0;
        final Drawable[] drawables = new Drawable[2];
        public int position;
        private ListItemMenu mListContextMenu = null;
        private final IconDeferredLoadingThread iconCache;

        boolean isStillValid(int layoutChangeId) {
            boolean hasChanged = this.layoutChangeId == layoutChangeId;
            this.layoutChangeId = layoutChangeId;
            return hasChanged;
        }

        ViewHolder(View convertView, ListItemMenu listContextMenu,
                   IconDeferredLoadingThread iconCache, int layoutChangeId) {
            this.layoutChangeId = layoutChangeId;
            mListContextMenu = listContextMenu;
            this.iconCache = iconCache;
            imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
            imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);
            seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
            //mainTextView = (LinearLayout) convertView.findViewById(R.id.outlet_list_text);
            entry = convertView.findViewById(R.id.item_layout);
            title = (TextView) convertView.findViewById(R.id.text1);
            subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        }

        @SuppressWarnings("SameParameterValue")
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

    static class DevicePortListItem {
        public final DevicePort port;
        final String displayText;
        // unique id for the gridView
        public final long id;
        // If you change a DevicePort's value, that new value may be stored in
        // command_value instead overwriting DevicePort's value. The implementation
        // depends on the child class.
        public int command_value;

        DevicePortListItem(DevicePort oi, int command_value, long id) {
            this.id = id;
            this.port = oi;
            this.command_value = command_value;
            displayText = oi.device.DeviceName + ": " + oi.getDescription();
        }

        public boolean isEnabled() {
            return port.last_command_timecode <= port.device.getUpdatedTime();
        }

    }

    final List<DevicePortListItem> all_outlets;
    private final LayoutInflater inflater;
    private UUID filterGroup = null;

    void setGroupFilter(UUID groupFilter) {
        this.filterGroup = groupFilter;
    }

    public int getLayoutRes() {
        return outlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.outlet_res_id = layout_res;
    }

    DevicePortsBaseAdapter(Context context, ListItemMenu mListContextMenu) {
        this.mListContextMenu = mListContextMenu;
        inflater = LayoutInflater.from(context);
        iconCache.start();
        all_outlets = new ArrayList<DevicePortListItem>();
    }

    /**
     * Call this to load AnelDeviceSwitch data from a scene.
     * This will not update the view.
     *
     * @param scene
     */
    public void loadItemsOfScene(Scene scene) {
        for (Scene.SceneItem sceneItem : scene.sceneItems) {
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(sceneItem.uuid);
            if (port == null) {
                continue;
            }
            addItem(port, sceneItem.command);
        }
    }

    int getItemPositionByUUid(UUID uuid) {
        if (uuid == null)
            return -1;

        int i = 0;
        for (DevicePortListItem info : all_outlets) {
            if (info.port.uuid.equals(uuid))
                return i;
            ++i;
        }

        return -1;
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
        if (convertView != null) {
            current_viewHolder = (ViewHolder) convertView.getTag();
            if (!current_viewHolder.isStillValid(layoutChangeId))
                current_viewHolder = null;
            else
                current_viewHolder.isNew = false;
        } else
            current_viewHolder = null;


        if (current_viewHolder == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            assert convertView != null;
            current_viewHolder = new ViewHolder(convertView, mListContextMenu, iconCache, layoutChangeId);
            convertView.setTag(current_viewHolder);
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

    public void addItem(DevicePort oi, int command_value) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

        // FilterGroup
        if (filterGroup != null) {
            if (!oi.groups.contains(filterGroup))
                return;
        }

        DevicePortListItem new_oi = new DevicePortListItem(oi, command_value, nextId++);

        boolean found = false;
        for (int i = 0; i < all_outlets.size(); ++i) {
            boolean behind_current = all_outlets.get(i).port.positionRequest > new_oi.port.positionRequest;

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

        notifyDataSetChanged();
        return not_reachable;
    }

    public void clear() {
        all_outlets.clear();
        ++layoutChangeId;
        notifyDataSetChanged();
    }

    public void removeAt(int position, boolean notifyDataChanged) {
        all_outlets.remove(position);
        if (notifyDataChanged)
            notifyDataSetChanged();
    }

    public void invalidateViewHolders() {
        ++layoutChangeId;
        notifyDataSetChanged();
    }
}
