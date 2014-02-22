package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    protected int outlet_res_id = R.layout.outlet_list_switch;
    protected boolean showHidden = true;

    protected static class DevicePortListItem {
        public DevicePort port;
        String displayText;
        // unique id for the gridview
        public long id;
        // If you change a DevicePort's value, that new value may be stored in
        // command_value instead overwritting DevicePort's value. The implementation
        // depends on the parent class.
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
    }

    protected List<DevicePortListItem> all_outlets;
    protected LayoutInflater inflater;
    private boolean temporary_ignore_positionRequest;

    protected DevicePortsBaseAdapter(Context context) {
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
            addOutlet(port, sceneItem.command);
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
        }
        DevicePortListItem info = all_outlets.get(position);

        TextView textView = (TextView) convertView.findViewById(R.id.outlet_list_text);
        textView.setEnabled(info.isEnabled());
        textView.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        textView.setAlpha(info.port.Hidden ? 0.5f : 1.0f);
        textView.setText(info.displayText);

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

    public void addOutlet(DevicePort oi, int command_value) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

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
            if (!device.reachable) {
                not_reachable.add(device);
                continue;
            }

            for (DevicePort oi : device.DevicePorts) {
                addOutlet(oi, DevicePort.TOGGLE);
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
