package oly.netpowerctrl.scenes;

import android.content.Context;
import android.view.View;

import com.wefika.flowlayout.FlowLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.executables.adapter.ExecutablesAdapter;
import oly.netpowerctrl.executables.adapter.InputExecutables;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;

;

/**
 * Created by david on 23.12.14.
 */
public class SceneElementsAssigning {
    private RecyclerViewWithAdapter<ExecutablesAdapter> availableElements;
    private SceneElementsInFlowLayout sceneElements;
    private AdapterSource availableData;
    private SceneElementsAdapter includedData;

    public SceneElementsAssigning(Context context, final DataService dataService, FlowLayout layout_included, View available,
                                  final SceneElementsChanged sceneElementsChanged, Scene scene) {
        availableData = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartAfterFirstQuery);
        availableData.addInput(new InputExecutables());
        final ExecutablesAdapter adapter_available =
                new ExecutablesAdapter(availableData, LoadStoreIconData.iconLoadingThread, R.layout.list_item_available_outlet);
        includedData = new SceneElementsAdapter();

        sceneElements = new SceneElementsInFlowLayout(layout_included,
                includedData, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                availableData.addItem(includedData.take(position).getExecutable(), Executable.TOGGLE);
                sceneElementsChanged.onSceneElementsChanged();
                return true;
            }
        });
        availableElements = new RecyclerViewWithAdapter<>(context,
                available, adapter_available, R.string.scene_create_helptext_available);


        // Add click listener to available list to move the clicked action
        // to the included list.
        availableElements.setOnItemClickListener(new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                Executable executable = availableData.getItem(position).getExecutable();
                if (executable == null)
                    return false;
                if (dataService.executables.findByUID(executable.getUid()) != executable) {
                    throw new RuntimeException("DevicePort not equal!");
                }
                availableData.removeAt(position);
                includedData.addItem(executable, Executable.TOGGLE);
                sceneElementsChanged.onSceneElementsChanged();
                return true;
            }
        }, null));

        includedData.loadItemsOfScene(dataService, scene);
        includedData.setMasterOfScene(scene);
        includedData.notifyDataSetChanged();

        // ToBeRemoved will be an ordered list of indecies to be removed
        int[] toBeRemoved = new int[availableData.mItems.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < availableData.mItems.size(); ++index) {
            for (ExecutableAdapterItem adapter_list_item : includedData.mItems) {
                if (adapter_list_item.getExecutable() != null &&
                        adapter_list_item.getExecutableUid().equals(availableData.mItems.get(index).getExecutableUid())) {
                    toBeRemoved[lenToBeRemoved] = index;
                    lenToBeRemoved++;
                }
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            availableData.removeAt(toBeRemoved[i]);
    }

    public void applyToScene(Scene scene) {
        scene.sceneItems = SceneFactory.sceneItemsFromList(includedData);
        scene.setMaster(includedData.getMaster());
    }

    public boolean hasElements() {
        return availableData.mItems.size() > 0;
    }

    public interface SceneElementsChanged {
        void onSceneElementsChanged();
    }
}
