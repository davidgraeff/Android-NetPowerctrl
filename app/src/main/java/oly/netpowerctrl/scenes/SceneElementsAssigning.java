package oly.netpowerctrl.scenes;

import android.view.View;

import com.wefika.flowlayout.FlowLayout;

import java.util.Set;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.executables.adapter.InputExecutables;
import oly.netpowerctrl.scenes.adapter.SceneElementsAdapter;
import oly.netpowerctrl.scenes.adapter.SceneElementsInFlowLayout;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

/**
 * For all scene element related functionality. The SceneElementsAddDialog is used for selecting
 * elements that are not part of the given Scene and uses the availableData and includedData
 * objects for that purpose.
 * Already included scene elements are shown by a flow layout encapsulated in SceneElementsInFlowLayout.
 *
 */
public class SceneElementsAssigning implements RecyclerItemClickListener.OnItemClickListener {
    private final SceneElementsChanged sceneElementsChanged;
    private SceneElementsInFlowLayout sceneElements;
    private AdapterSource availableData;
    private SceneElementsAdapter includedData;

    public SceneElementsAssigning(final DataService dataService, FlowLayout layout_included,
                                  SceneElementsChanged sceneElementsChanged, Scene scene) {
        this.sceneElementsChanged = sceneElementsChanged;
        availableData = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartAfterFirstQuery);
        availableData.addInput(new InputExecutables());
        includedData = new SceneElementsAdapter();

        sceneElements = new SceneElementsInFlowLayout(layout_included, includedData, this);

        includedData.loadItemsOfScene(dataService, scene);

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

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        availableData.addItem(includedData.take(position).getExecutable(), Executable.TOGGLE);
        sceneElementsChanged.onSceneElementsChanged();
        return true;
    }

    public void applyToScene(Scene scene) {
        scene.sceneItems = SceneFactory.sceneItemsFromList(includedData);
        scene.setMaster(includedData.getMasterExecutable());
    }

    public AdapterSource getAdaperSourceAvailable() {
        return availableData;
    }

    public void addToScene(Set<String> executableUIDSet) {
        for (String executableUID : executableUIDSet) {
            int pos = availableData.findPositionByUUid(executableUID);
            Executable executable = availableData.getItem(pos).getExecutable();
            availableData.removeAt(pos);
            includedData.addItem(executable, Executable.TOGGLE);
            sceneElementsChanged.onSceneElementsChanged();
        }
    }

    public boolean hasElements() {
        return includedData.getItemCount() > 0;
    }

    public interface SceneElementsChanged {
        void onSceneElementsChanged();
    }
}
