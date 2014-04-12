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
import oly.netpowerctrl.utils.SegmentedRadioGroup;

public class DevicePortsCreateSceneAdapter extends DevicePortsBaseAdapter {
    private View.OnClickListener closeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListItemMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };

    private RadioGroup.OnCheckedChangeListener switchClickListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int position = (Integer) radioGroup.getTag();
            all_outlets.get(position).command_value = (i == R.id.radio0) ? DevicePort.OFF :
                    (i == R.id.radio1 ? DevicePort.ON : DevicePort.TOGGLE);
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
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
        super(context, null, null);
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

            SegmentedRadioGroup rGroup = (SegmentedRadioGroup) convertView.findViewById(R.id.radioGroup);
            rGroup.setOnCheckedChangeListener(switchClickListener);
            rGroup.setTag(position);
            RadioButton r0 = (RadioButton) convertView.findViewById(R.id.radio0);
            RadioButton r1 = (RadioButton) convertView.findViewById(R.id.radio1);
            RadioButton r2 = (RadioButton) convertView.findViewById(R.id.radio2);

            switch (info.command_value) {
                case DevicePort.OFF:
                    r0.setChecked(true);
                    break;
                case DevicePort.ON:
                    r1.setChecked(true);
                    break;
                case DevicePort.TOGGLE:
                    r2.setChecked(true);
                    break;
            }

        } else if (type == DevicePort.DevicePortType.TypeRangedValue) {
            outlet_res_id = R.layout.create_scene_outlet_list_ranged;
            convertView = super.getView(position, convertView, parent);
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
}
