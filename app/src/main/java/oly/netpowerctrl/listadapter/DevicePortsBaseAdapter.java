package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Executor;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsBaseAdapter extends AbstractDynamicGridAdapter {

    protected ListItemMenu mListItemMenu = null;
    protected int nextId = 0; // we need stable IDs for the gridView
    protected int outlet_res_id = R.layout.outlet_list_item;
    protected boolean showHidden = true;
    protected ViewHolder current_viewHolder;

    // Some observers
    private ListItemMenu mListContextMenu = null;

    //ViewHolder pattern
    protected static class ViewHolder implements View.OnClickListener {
        ImageView imageView;
        //LinearLayout mainTextView;
        View entry;
        SeekBar seekBar;
        TextView portName;
        TextView deviceName;
        boolean isNew = true;

        Bitmap bitmapDefault;
        Bitmap bitmapOn;
        Bitmap bitmapOff;
        public int position;
        private ListItemMenu mListContextMenu = null;

        ViewHolder(View convertView, ListItemMenu listContextMenu) {
            mListContextMenu = listContextMenu;
            imageView = (ImageView) convertView.findViewById(R.id.outlet_list_bitmap);
            seekBar = (SeekBar) convertView.findViewById(R.id.outlet_list_seekbar);
            //mainTextView = (LinearLayout) convertView.findViewById(R.id.outlet_list_text);
            entry = convertView.findViewById(R.id.outlet_list_entry);
            portName = (TextView) convertView.findViewById(R.id.outlet_list_portname);
            deviceName = (TextView) convertView.findViewById(R.id.outlet_list_text_devicename);
        }

        @Override
        public void onClick(View view) {
            mListContextMenu.onMenuItemClicked(view, position);
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

    protected DevicePortsBaseAdapter(Context context, ListItemMenu mListContextMenu, UUID filterGroup) {
        this.mListContextMenu = mListContextMenu;
        this.context = context;
        this.filterGroup = filterGroup;
        inflater = LayoutInflater.from(context);
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

    public List<Executor.PortAndCommand> getDeviceCommands() {
        List<Executor.PortAndCommand> list_of_scene_outlets = new ArrayList<Executor.PortAndCommand>();
        for (DevicePortListItem info : all_outlets) {
            Executor.PortAndCommand p = new Executor.PortAndCommand();
            p.port = info.port;
            p.command = info.command_value;
            list_of_scene_outlets.add(p);
        }
        return list_of_scene_outlets;
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
            current_viewHolder = new ViewHolder(convertView, mListContextMenu);
            convertView.setTag(current_viewHolder);
        } else {
            current_viewHolder = (ViewHolder) convertView.getTag();
            current_viewHolder.isNew = false;
        }
        current_viewHolder.position = position;

        DevicePortListItem info = all_outlets.get(position);

        current_viewHolder.entry.setAlpha(info.port.Hidden ? 0.5f : 1.0f);
        current_viewHolder.entry.setEnabled(info.isEnabled());

        current_viewHolder.portName.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_viewHolder.portName.setText(info.port.getDescription());
        current_viewHolder.portName.setEnabled(info.isEnabled());

        current_viewHolder.deviceName.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_viewHolder.deviceName.setText(info.port.device.DeviceName);
        current_viewHolder.deviceName.setEnabled(info.isEnabled());

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

            //TODO crash concurrent access
            for (DevicePort oi : device.DevicePorts) {
                addItem(oi, oi.current_value);
            }
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
