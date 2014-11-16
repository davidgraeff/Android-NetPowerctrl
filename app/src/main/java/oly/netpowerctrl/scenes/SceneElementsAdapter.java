package oly.netpowerctrl.scenes;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.device_base.executables.ExecutableType;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.ui.widgets.SegmentedRadioGroup;

public class SceneElementsAdapter extends RecyclerView.Adapter<SceneElementsAdapter.ViewHolder> {
    public final List<ExecutableAdapterItem> mItems = new ArrayList<>();

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
            mItems.get(position).setCommand_value(seekBar.getProgress());
        }
    };

    private int mNextId = 0; // we need stable IDs
    private ExecutableAdapterItem master = null;
    private final RadioGroup.OnCheckedChangeListener switchClickListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int position = (Integer) radioGroup.getTag();
            ExecutableAdapterItem info = mItems.get(position);
            int command_value = info.getCommand_value();

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

            info.setCommand_value(command_value);

            if (masterChanged)
                notifyDataSetChanged();
        }
    };

    @Override
    public int getItemViewType(int position) {
        final ExecutableAdapterItem item = mItems.get(position);
        return item.getItemViewType();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType >= 100) {
            // Fatal error -> No groups allowed in scene elements!
            return null;
        }

        ExecutableType type = ExecutableType.values()[viewType];
        View view;

        switch (type) {
            case TypeRangedValue:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_edit_scene_ranged, viewGroup, false);
                break;
            case TypeToggle:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_edit_scene_switch, viewGroup, false);
                break;
            case TypeStateless:
            case TypeUnknown:
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_edit_scene, viewGroup, false);
                break;
        }

        return new ViewHolder(view, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        ExecutableAdapterItem item = mItems.get(position);
        DevicePort port = (DevicePort) item.getExecutable();

        viewHolder.title.setText(port.getTitle());
        viewHolder.title.setEnabled(port.isEnabled());

        if (viewHolder.subtitle != null) {
            viewHolder.subtitle.setText(port.device.getDeviceName());
        }

        ExecutableType type = port.getType();
        if (type == ExecutableType.TypeToggle) {
            viewHolder.rGroup.setOnCheckedChangeListener(null);
            viewHolder.rGroup.setTag(position);

            if (master == null || item.equals(master)) {
                viewHolder.r2.setText(R.string.toggle);
            } else {
                viewHolder.r2.setText(R.string.toggleSlave);
            }

            switch (item.getCommand_value()) {
                case DevicePort.OFF:
                    viewHolder.r0.setChecked(true);
                    break;
                case DevicePort.ON:
                    viewHolder.r1.setChecked(true);
                    break;
                case DevicePort.TOGGLE:
                    if (item.equals(master))
                        viewHolder.r3.setChecked(true);
                    else
                        viewHolder.r2.setChecked(true);
                    break;
            }

            viewHolder.rGroup.setOnCheckedChangeListener(switchClickListener);
        } else if (type == ExecutableType.TypeRangedValue) {
            viewHolder.seekBar.setTag(position);
            viewHolder.seekBar.setMax(port.max_value);
            viewHolder.seekBar.setProgress(item.getCommand_value());
            viewHolder.seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void switchAllOn() {
        for (ExecutableAdapterItem item : mItems) {
            DevicePort port = (DevicePort) item.getExecutable();
            item.setCommand_value(port.max_value);
        }
        notifyDataSetChanged();
    }

    public void switchAllOff() {
        for (ExecutableAdapterItem item : mItems) {
            DevicePort port = (DevicePort) item.getExecutable();
            item.setCommand_value(port.min_value);
        }
        notifyDataSetChanged();
    }

    public void toggleAll() {
        for (ExecutableAdapterItem outlet_info : mItems) {
            outlet_info.setCommand_value(DevicePort.TOGGLE);
        }
        notifyDataSetChanged();
    }

    public DevicePort getMaster() {
        if (master != null)
            return (DevicePort) master.getExecutable();
        return null;
    }

    public void setMasterOfScene(Scene scene) {
        if (scene.isMasterSlave()) {
            int p = findPositionByUUid(scene.getMasterUUid());
            if (p != -1)
                master = mItems.get(p);
            else
                master = null;
        }
    }

    private int findPositionByUUid(String uuid) {
        if (uuid == null)
            return -1;

        int i = -1;
        for (ExecutableAdapterItem info : mItems) {
            ++i;
            String uid = info.getExecutableUid();
            if (uid == null) // skip header items
                continue;
            if (uid.equals(uuid))
                return i;
        }

        return -1;
    }

    /**
     * Call this to load device ports from a scene.
     * This will not update the view.
     *
     * @param scene
     */
    public void loadItemsOfScene(Scene scene) {
        for (SceneItem sceneItem : scene.sceneItems) {
            DevicePort port = AppData.getInstance().findDevicePort(sceneItem.uuid);
            if (port == null) {
                continue;
            }
            addItem(port, sceneItem.command);
        }
    }

    public void clear() {
        int all = mItems.size();
        if (all == 0) return;
        mItems.clear();
        notifyItemRangeRemoved(0, all - 1);
    }

    public ExecutableAdapterItem take(int position) {
        ExecutableAdapterItem item = mItems.get(position);
        mItems.remove(position);
        notifyItemRemoved(position);
        return item;
    }

    public void addItem(Executable executable, int command) {
        int position = findPositionByUUid(executable.getUid());
        if (position == -1) {
            mItems.add(new ExecutableAdapterItem(executable, command, ++mNextId));
            notifyItemInserted(mItems.size() - 1);
        } else {
            ExecutableAdapterItem executableAdapterItem = mItems.get(position);
            executableAdapterItem.setCommand_value(command);
            executableAdapterItem.setExecutable(executable);
            notifyItemChanged(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View entry;
        final View close;
        final TextView title;
        final TextView subtitle;
        public int position;

        SegmentedRadioGroup rGroup;
        RadioButton r0;
        RadioButton r1;
        RadioButton r2;
        RadioButton r3;

        SeekBar seekBar;

        ViewHolder(View convertView, ExecutableType type) {
            super(convertView);

            entry = convertView.findViewById(R.id.item_layout);
            title = (TextView) convertView.findViewById(R.id.title);
            subtitle = (TextView) convertView.findViewById(R.id.subtitle);
            close = convertView.findViewById(R.id.outlet_list_close);

            switch (type) {
                case TypeToggle:
                    rGroup = (SegmentedRadioGroup) convertView.findViewById(R.id.radioGroup);
                    r0 = (RadioButton) convertView.findViewById(R.id.radioSwitchOff);
                    r1 = (RadioButton) convertView.findViewById(R.id.radioSwitchOn);
                    r2 = (RadioButton) convertView.findViewById(R.id.radioToggle);
                    r3 = (RadioButton) convertView.findViewById(R.id.radioToggleMaster);
                    break;
                case TypeRangedValue:
                    seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
                    break;
            }

            seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);

        }
    }

}
