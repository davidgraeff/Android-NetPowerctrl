package oly.netpowerctrl.scenes.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneItem;

public class SceneElementsAdapter extends RecyclerView.Adapter<SceneElementViewHolder> implements ChangeMasterInterface {
    public final List<ExecutableAdapterItem> mItems = new ArrayList<>();
    private int mNextId = 0; // we need stable IDs
    private ExecutableAdapterItem master = null;

    @Override
    public int getItemViewType(int position) {
        final ExecutableAdapterItem item = mItems.get(position);
        return item.getItemViewType();
    }

    @Override
    public SceneElementViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
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

        return new SceneElementViewHolder(view, type);
    }

    @Override
    public void onBindViewHolder(SceneElementViewHolder viewHolder, int position) {
        ExecutableAdapterItem item = mItems.get(position);
        Executable port = item.getExecutable();

        viewHolder.title.setText(port.getTitle());
        viewHolder.title.setEnabled(!port.executionInProgress());

        if (viewHolder.subtitle != null) {
            viewHolder.subtitle.setText(port.getDescription());
        }

        ExecutableType type = port.getType();
        if (type == ExecutableType.TypeToggle) {
            viewHolder.rGroup.setOnCheckedChangeListener(null);

            if (master == null || item.equals(master)) {
                viewHolder.r2.setText(R.string.toggle);
            } else {
                viewHolder.r2.setText(R.string.toggleSlave);
            }

            switch (item.getCommandValue()) {
                case ExecutableAndCommand.OFF:
                    viewHolder.r0.setChecked(true);
                    break;
                case ExecutableAndCommand.ON:
                    viewHolder.r1.setChecked(true);
                    break;
                case ExecutableAndCommand.TOGGLE:
                    if (item == master)
                        viewHolder.r3.setChecked(true);
                    else
                        viewHolder.r2.setChecked(true);
                    break;
            }

            viewHolder.rGroup.setOnCheckedChangeListener(new CheckChangeListener(item, this));
        } else if (type == ExecutableType.TypeRangedValue) {
            viewHolder.seekBar.setMax(port.max_value);
            viewHolder.seekBar.setProgress(item.getCommandValue());
            viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener(item));
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void switchAllOn() {
        for (ExecutableAdapterItem item : mItems) {
            Executable port = (Executable) item.getExecutable();
            item.setCommandValue(port.max_value);
        }
        notifyDataSetChanged();
    }

    public void switchAllOff() {
        for (ExecutableAdapterItem item : mItems) {
            Executable executable = item.getExecutable();
            item.setCommandValue(executable.min_value);
        }
        notifyDataSetChanged();
    }

    public void toggleAll() {
        for (ExecutableAdapterItem outlet_info : mItems) {
            outlet_info.setCommandValue(ExecutableAndCommand.TOGGLE);
        }
        notifyDataSetChanged();
    }

    @Override
    public ExecutableAdapterItem getMaster() {
        return master;
    }

    @Override
    public void setMaster(ExecutableAdapterItem item) {
        if (item == master) return;
        master = item;
        notifyDataSetChanged();
    }

    public Executable getMasterExecutable() {
        if (master != null)
            return master.getExecutable();
        return null;
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
     *
     * @param scene The scene
     */
    public void loadItemsOfScene(DataService dataService, Scene scene) {
        for (SceneItem sceneItem : scene.sceneItems) {
            Executable executable = dataService.executables.findByUID(sceneItem.uuid);
            if (executable == null) {
                continue;
            }
            mItems.add(new ExecutableAdapterItem(executable, sceneItem.command, ++mNextId));
        }

        if (scene.isMasterSlave()) {
            int p = findPositionByUUid(scene.getMasterExecutableUid());
            if (p != -1)
                master = mItems.get(p);
            else
                master = null;
        }

        notifyDataSetChanged();
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
            executableAdapterItem.setCommandValue(command);
            executableAdapterItem.setExecutable(executable);
            notifyItemChanged(position);
        }
    }

}
