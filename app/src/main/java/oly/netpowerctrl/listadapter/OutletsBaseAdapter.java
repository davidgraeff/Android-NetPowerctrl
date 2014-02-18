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
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.utils.ListItemMenu;

public class OutletsBaseAdapter extends AbstractDynamicGridAdapter {

    protected ListItemMenu mListItemMenu = null;
    protected int nextId = 0; // we need stable IDs for the gridView
    protected int outlet_res_id = R.layout.outlet_list_switch;
    protected boolean showHidden = true;

    protected static class OutletInfoAdditional {
        public OutletInfo oi;
        String displayText;
        public boolean enabled;
        public long id;

        OutletInfoAdditional(OutletInfo oi, boolean showDevice, boolean enabled, long id) {
            this.id = id;
            this.oi = oi;
            this.enabled = enabled; //oi.device.reachable;
            if (showDevice)
                displayText = oi.device.DeviceName + ": " + oi.getDescription();
            else
                displayText = oi.getDescription();
        }
    }

    protected List<OutletInfoAdditional> all_outlets;
    protected LayoutInflater inflater;
    private boolean temporary_ignore_positionRequest;

    protected OutletsBaseAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfoAdditional>();
    }

    /**
     * Call this to load OutletInfo data from a scene.
     *
     * @param scene
     */
    public void loadByScene(Scene scene) {
        for (SceneOutlet scene_outlet : scene.sceneOutlets) {
            if (!scene_outlet.updateDeviceAndOutletLinks()) {
                continue;
            }
            addOutlet(scene_outlet.outletinfo);
        }
        notifyDataSetChanged();
    }

    public List<SceneOutlet> getDeviceCommands() {
        List<SceneOutlet> list_of_scene_outlets = new ArrayList<SceneOutlet>();
        for (OutletInfoAdditional info : all_outlets) {
            list_of_scene_outlets.add(SceneOutlet.fromOutletInfo(info.oi, true));
        }
        return list_of_scene_outlets;
    }

    public void setListItemMenu(ListItemMenu listItemMenu) {
        this.mListItemMenu = listItemMenu;
    }

    @Override
    public int getCount() {
        return all_outlets.size();
    }

    public OutletInfo getItem(int position) {
        return all_outlets.get(position).oi;
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
        OutletInfoAdditional info = all_outlets.get(position);

        TextView textView = (TextView) convertView.findViewById(R.id.outlet_list_text);
        textView.setEnabled(info.enabled);
        textView.setTypeface(null, info.oi.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        textView.setAlpha(info.oi.Hidden ? 0.5f : 1.0f);
        textView.setText(info.displayText);

        return convertView;
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition >= getCount()) {
            return;
        }
        OutletInfoAdditional temp = all_outlets.get(originalPosition);
        all_outlets.get(originalPosition).oi.positionRequest = all_outlets.get(newPosition).oi.positionRequest;
        all_outlets.get(newPosition).oi.positionRequest = temp.oi.positionRequest;
        all_outlets.remove(originalPosition);
        all_outlets.add(newPosition, temp);
        notifyDataSetChanged();
    }

    @Override
    public void finishedReordering() {
        NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
    }

    public void addOutlet(OutletInfo oi) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

        OutletInfoAdditional new_oi = new OutletInfoAdditional(oi, true, true, nextId++);

        boolean found = false;
        for (int i = 0; i < all_outlets.size(); ++i) {
            boolean behind_current = temporary_ignore_positionRequest ?
                    all_outlets.get(i).displayText.compareTo(new_oi.displayText) > 0 :
                    all_outlets.get(i).oi.positionRequest > new_oi.oi.positionRequest;

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

            for (OutletInfo oi : device.Outlets) {
                addOutlet(oi);
            }

            for (OutletInfo oi : device.IOs) {
                addOutlet(oi);
            }
        }

        // Sort for positionRequest number or alphabetically
        //Collections.sort(all_outlets);

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(i).oi.positionRequest = i;
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
