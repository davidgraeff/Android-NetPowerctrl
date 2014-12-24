package oly.netpowerctrl.scenes;

import android.content.Context;
import android.view.View;

import com.wefika.flowlayout.FlowLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.AdapterSourceInputDevicePorts;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutablesListAdapter;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;

/**
 * Created by david on 23.12.14.
 */
public class SceneElementsAssigning {
    RecyclerViewWithAdapter<ExecutablesListAdapter> availableElements;
    SceneElementsInFlowLayout sceneElements;
    private ExecutablesListAdapter adapter_available;
    private SceneElementsAdapter adapter_included;

    public SceneElementsAssigning(Context context, final AppData appData, FlowLayout layout_included, View available,
                                  final SceneElementsChanged sceneElementsChanged, Scene scene) {
        AdapterSource adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartAfterFirstQuery);
        adapterSource.add(new AdapterSourceInputDevicePorts());
        adapter_available = new ExecutablesListAdapter(false, adapterSource, LoadStoreIconData.iconLoadingThread, true);
        adapter_included = new SceneElementsAdapter();

        sceneElements = new SceneElementsInFlowLayout(layout_included,
                adapter_included, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                adapter_available.addItem(adapter_included.take(position).getExecutable(), DevicePort.TOGGLE);
                sceneElementsChanged.onSceneElementsChanged();
                return true;
            }
        });
        availableElements = new RecyclerViewWithAdapter<>(context, null,
                available, adapter_available, R.string.scene_create_helptext_available);


        // Add click listener to available list to move the clicked action
        // to the included list.
        availableElements.setOnItemClickListener(new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                DevicePort devicePort = (DevicePort) adapter_available.getItem(position).getExecutable();
                if (devicePort == null)
                    return false;
                if (appData.findDevicePort(devicePort.getUid()) != devicePort) {
                    throw new RuntimeException("DevicePort not equal!");
                }
                adapter_available.removeAt(position);
                adapter_included.addItem(devicePort, DevicePort.TOGGLE);
                sceneElementsChanged.onSceneElementsChanged();
                return true;
            }
        }, null));

        adapter_included.loadItemsOfScene(appData, scene);
        adapter_included.setMasterOfScene(scene);
        adapter_included.notifyDataSetChanged();

        // ToBeRemoved will be an ordered list of indecies to be removed
        int[] toBeRemoved = new int[adapter_available.mItems.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < adapter_available.mItems.size(); ++index) {
            for (ExecutableAdapterItem adapter_list_item : adapter_included.mItems) {
                if (adapter_list_item.getExecutable() != null &&
                        adapter_list_item.getExecutableUid().equals(adapter_available.mItems.get(index).getExecutableUid())) {
                    toBeRemoved[lenToBeRemoved] = index;
                    lenToBeRemoved++;
                }
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            adapter_available.removeAt(toBeRemoved[i]);
    }

    public void applyToScene(Scene scene) {
        scene.sceneItems = SceneFactory.sceneItemsFromList(adapter_included);
        scene.setMaster(adapter_included.getMaster());
    }

    public boolean hasElements() {
        return adapter_available.getItemCount() > 0;
    }

    public interface SceneElementsChanged {
        void onSceneElementsChanged();
    }
}
