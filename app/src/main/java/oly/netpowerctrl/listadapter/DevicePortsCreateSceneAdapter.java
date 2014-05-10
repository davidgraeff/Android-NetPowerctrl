package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.utils.SegmentedRadioGroup;

public class DevicePortsCreateSceneAdapter extends DevicePortsBaseAdapter {
    private DevicePortListItem master = null;

    private final View.OnClickListener closeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListItemMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };

    private final RadioGroup.OnCheckedChangeListener switchClickListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int position = (Integer) radioGroup.getTag();
            DevicePortListItem info = all_outlets.get(position);
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
            all_outlets.get(position).command_value = seekBar.getProgress();
        }
    };

    public DevicePortsCreateSceneAdapter(Context context) {
        super(context, null);
    }

    public int getViewTypeCount() {
        return DevicePort.DevicePortType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return all_outlets.get(position).port.getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem info = all_outlets.get(position);
        DevicePort.DevicePortType type = info.port.getType();
        if (type == DevicePort.DevicePortType.TypeToggle) {
            outlet_res_id = R.layout.create_scene_outlet_list_switch;
            convertView = super.getView(position, convertView, parent);

            assert convertView != null;
            SegmentedRadioGroup rGroup = (SegmentedRadioGroup) convertView.findViewById(R.id.radioGroup);
            rGroup.setOnCheckedChangeListener(switchClickListener);
            rGroup.setTag(position);
            RadioButton r0 = (RadioButton) convertView.findViewById(R.id.radioSwitchOff);
            RadioButton r1 = (RadioButton) convertView.findViewById(R.id.radioSwitchOn);
            RadioButton r2 = (RadioButton) convertView.findViewById(R.id.radioToggle);
            RadioButton r3 = (RadioButton) convertView.findViewById(R.id.radioToggleMaster);

            if (master == null || info.equals(master)) {
                r2.setText(R.string.toggle);
            } else {
                r2.setText(R.string.toggleSlave);
            }

            switch (info.command_value) {
                case DevicePort.OFF:
                    r0.setChecked(true);
                    break;
                case DevicePort.ON:
                    r1.setChecked(true);
                    break;
                case DevicePort.TOGGLE:
                    if (info.equals(master))
                        r3.setChecked(true);
                    else
                        r2.setChecked(true);
                    break;
            }

        } else if (type == DevicePort.DevicePortType.TypeRangedValue) {
            outlet_res_id = R.layout.create_scene_outlet_list_ranged;
            convertView = super.getView(position, convertView, parent);
            assert convertView != null;
            SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
            //current_viewHolder.seekBar
            seekBar.setTag(position);
            seekBar.setMax(info.port.max_value);
            seekBar.setProgress(info.command_value);
            seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        } else if (type == DevicePort.DevicePortType.TypeButton) {
            outlet_res_id = R.layout.create_scene_outlet_list_item;
            convertView = super.getView(position, convertView, parent);

        } else {
            outlet_res_id = R.layout.create_scene_outlet_list_item;
            convertView = super.getView(position, convertView, parent);
        }

        assert convertView != null;
        ImageView btnClose = (ImageView) convertView.findViewById(R.id.outlet_list_close);
        if (mListItemMenu != null) {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setTag(position);
            btnClose.setOnClickListener(closeClickListener);
        } else {
            btnClose.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void switchAllOn() {
        for (DevicePortListItem outlet_info : all_outlets) {
            outlet_info.command_value = outlet_info.port.max_value;
        }
        notifyDataSetChanged();
    }

    public void switchAllOff() {
        for (DevicePortListItem outlet_info : all_outlets) {
            outlet_info.command_value = outlet_info.port.min_value;
        }
        notifyDataSetChanged();
    }

    public void toggleAll() {
        for (DevicePortListItem outlet_info : all_outlets) {
            outlet_info.command_value = DevicePort.TOGGLE;
        }
        notifyDataSetChanged();
    }

    public void removeAll(DevicePortsCreateSceneAdapter adapter) {
        for (DevicePortListItem outlet_info : adapter.all_outlets) {
            Iterator<DevicePortListItem> i = all_outlets.iterator();
            while (i.hasNext()) {
                if (outlet_info.port.equals(i.next().port)) {
                    i.remove();
                }
            }

        }
    }

    public DevicePort getItem(int position) {
        return all_outlets.get(position).port;
    }

    public DevicePort getMaster() {
        if (master != null)
            return master.port;
        return null;
    }

    public void setMasterOfScene(Scene scene) {
        if (scene.isMasterSlave()) {
            int p = getItemPositionByUUid(scene.getMasterUUid());
            if (p != -1)
                master = all_outlets.get(p);
            else
                master = null;
        }
    }
}
