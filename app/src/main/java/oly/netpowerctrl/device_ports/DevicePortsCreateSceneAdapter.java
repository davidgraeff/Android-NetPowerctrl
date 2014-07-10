package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.gui.SegmentedRadioGroup;

public class DevicePortsCreateSceneAdapter extends DevicePortsBaseAdapter {
    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListContextMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };
    private final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int position = (Integer) seekBar.getTag();
            mItems.get(position).command_value = seekBar.getProgress();
        }
    };
    private DevicePortListItem master = null;
    private final RadioGroup.OnCheckedChangeListener switchClickListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int position = (Integer) radioGroup.getTag();
            DevicePortListItem info = mItems.get(position);
            int command_value = info.command_value;

            boolean masterChanged = false;
            if (info.equals(master)) {
                masterChanged = true;
                master = null;
            }

            switch (i) {
                case R.id.radioSwitchOff:
                    command_value = DevicePort.OFF;
                    break;
                case R.id.radioSwitchOn:
                    command_value = DevicePort.ON;
                    break;
                case R.id.radioToggleMaster:
                    master = info;
                    masterChanged = true;
                    // no break
                case R.id.radioToggle:
                    command_value = DevicePort.TOGGLE;
                    break;
            }

            info.command_value = command_value;

            if (masterChanged)
                notifyDataSetChanged();
        }
    };

    public DevicePortsCreateSceneAdapter(Context context, IconDeferredLoadingThread iconCache) {
        super(context, null, null, iconCache, false);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem item = mItems.get(position);
        DevicePort port = item.port;

        // Not our business, if port is null
        if (port == null) {
            mOutlet_res_id = R.layout.create_scene_outlet_list_item;
            convertView = super.getView(position, convertView, parent);
            return convertView;
        }

        DevicePort.DevicePortType type = port.getType();
        if (type == DevicePort.DevicePortType.TypeToggle) {
            mOutlet_res_id = R.layout.create_scene_outlet_list_switch;

            assert convertView != null;
            SegmentedRadioGroup rGroup = (SegmentedRadioGroup) convertView.findViewById(R.id.radioGroup);
            rGroup.setOnCheckedChangeListener(switchClickListener);
            rGroup.setTag(position);
            RadioButton r0 = (RadioButton) convertView.findViewById(R.id.radioSwitchOff);
            RadioButton r1 = (RadioButton) convertView.findViewById(R.id.radioSwitchOn);
            RadioButton r2 = (RadioButton) convertView.findViewById(R.id.radioToggle);
            RadioButton r3 = (RadioButton) convertView.findViewById(R.id.radioToggleMaster);

            if (master == null || item.equals(master)) {
                r2.setText(R.string.toggle);
            } else {
                r2.setText(R.string.toggleSlave);
            }

            switch (item.command_value) {
                case DevicePort.OFF:
                    r0.setChecked(true);
                    break;
                case DevicePort.ON:
                    r1.setChecked(true);
                    break;
                case DevicePort.TOGGLE:
                    if (item.equals(master))
                        r3.setChecked(true);
                    else
                        r2.setChecked(true);
                    break;
            }

        } else if (type == DevicePort.DevicePortType.TypeRangedValue) {
            mOutlet_res_id = R.layout.create_scene_outlet_list_ranged;
            convertView = super.getView(position, convertView, parent);
            assert convertView != null;
            SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
            //current_viewHolder.seekBar
            seekBar.setTag(position);
            seekBar.setMax(port.max_value);
            seekBar.setProgress(item.command_value);
            seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        } else if (type == DevicePort.DevicePortType.TypeButton) {
            mOutlet_res_id = R.layout.create_scene_outlet_list_item;
            convertView = super.getView(position, convertView, parent);

        } else {
            mOutlet_res_id = R.layout.create_scene_outlet_list_item;
            convertView = super.getView(position, convertView, parent);
        }

        assert convertView != null;
        ImageView btnClose = (ImageView) convertView.findViewById(R.id.outlet_list_close);
        if (mListContextMenu != null) {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setTag(position);
            btnClose.setOnClickListener(clickListener);
        } else {
            btnClose.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void switchAllOn() {
        for (DevicePortListItem outlet_info : mItems) {
            outlet_info.command_value = outlet_info.port.max_value;
        }
        notifyDataSetChanged();
    }

    public void switchAllOff() {
        for (DevicePortListItem outlet_info : mItems) {
            outlet_info.command_value = outlet_info.port.min_value;
        }
        notifyDataSetChanged();
    }

    public void toggleAll() {
        for (DevicePortListItem outlet_info : mItems) {
            outlet_info.command_value = DevicePort.TOGGLE;
        }
        notifyDataSetChanged();
    }

    public DevicePort getMaster() {
        if (master != null)
            return master.port;
        return null;
    }

    public void setMasterOfScene(Scene scene) {
        if (scene.isMasterSlave()) {
            int p = findIndexByUUid(scene.getMasterUUid());
            if (p != -1)
                master = mItems.get(p);
            else
                master = null;
        }
    }
}
