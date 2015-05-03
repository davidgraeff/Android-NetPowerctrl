package oly.netpowerctrl.scenes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

public class Scene extends Executable implements IOInterface {
    private static final String TAG = "SCENE";
    private static long nextStableID = 0;
    //public Bitmap bitmap = null;
    public final long id = nextStableID++;
    public List<SceneItem> sceneItems = new ArrayList<>();
    private String uuid_master = null;
    private ReachabilityStates reachable = ReachabilityStates.MaybeReachable;

    public onCollectionUpdated<ExecutableCollection, Executable> executableObserver = new onCollectionUpdated<ExecutableCollection, Executable>() {
        @Override
        public boolean updated(@NonNull ExecutableCollection c, @Nullable Executable executable, @NonNull ObserverUpdateActions action) {
            if (executable == null || (action != ObserverUpdateActions.UpdateAction && action != ObserverUpdateActions.UpdateReachableAction))
                return true;

            if (getMasterExecutableUid().equals(executable.getUid()) && reachable != executable.reachableState()) {
                reachable = executable.reachableState();
                c.put(Scene.this);
            }
            return true;
        }
    };

    /**
     * Create an invalid scene. Do not use that constructor, it is for instantiating per reflection only!
     */
    public Scene() {
        ui_type = ExecutableType.TypeStateless;
        min_value = 0;
    }

    public static Scene createNewScene() {
        Scene scene = new Scene();
        scene.uid = UUID.randomUUID().toString();
        return scene;
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param dataService DataService
     * @param callback    The callback for the execution-done messages
     */
    public void execute(@NonNull final DataService dataService, final onExecutionFinished callback) {
        List<AbstractBasePlugin> abstractBasePlugins = new ArrayList<>();

        // Master/Slave
        SceneItem masterItem = getMasterSceneItem();
        int master_command = Executable.INVALID;
        if (masterItem != null) {
            // If the command is not toggle, we return it now. It can be applied to slaves
            // directly.
            if (masterItem.command != Executable.TOGGLE) {
                master_command = masterItem.command;
            } else {
                // If the command is toggle, we have to find out the final command.
                Executable port = dataService.executables.findByUID(masterItem.uuid);
                if (port == null)
                    master_command = Executable.INVALID;
                else
                    master_command = port.getCurrentValueToggled();
            }
        }

        int countValidSceneItems = 0;
        for (SceneItem item : sceneItems) {
            Executable executable = dataService.executables.findByUID(item.uuid);
            if (executable == null) {
                Log.e(TAG, "Execute scene, DevicePort not found " + item.uuid);
                continue;
            }

            ++countValidSceneItems;

            AbstractBasePlugin remote = executable.getCredentials().getPlugin();
            if (remote == null) {
                Log.e(TAG, "Execute scene, PluginInterface not found " + item.uuid);
                continue;
            }

            int command = item.command;
            // Replace toggle by master command if master is set
            if (master_command != Executable.INVALID && item.command == Executable.TOGGLE)
                command = master_command;

            remote.addToTransaction(executable, command);
            if (!abstractBasePlugins.contains(remote))
                abstractBasePlugins.add(remote);
        }

        callback.addExpected(countValidSceneItems);

        for (AbstractBasePlugin p : abstractBasePlugins) {
            p.executeTransaction(callback);
        }
    }

    @Override
    public void execute(@NonNull DataService dataService, int command, onExecutionFinished callback) {
        execute(dataService, callback);
    }

    public void setMaster(Executable master) {
        updateMaster((master != null) ? master.getUid() : null);
    }

    private void updateMaster(String uuid_master) {
        this.uuid_master = uuid_master;
        if (isMasterSlave()) {
            ui_type = ExecutableType.TypeToggle;
            DataService.getService().executables.registerObserver(executableObserver);
        } else {
            ui_type = ExecutableType.TypeStateless;
            DataService.getService().executables.unregisterObserver(executableObserver);
        }
    }

    public String getMasterExecutableUid() {
        return uuid_master;
    }

    public SceneItem getMasterSceneItem() {
        if (uuid_master == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid_master)) {
                return item;
            }
        return null;
    }

    public boolean isMasterSlave() {
        return (uuid_master != null);
    }

    public void add(String action_uuid, int command) {
        sceneItems.add(new SceneItem(action_uuid, command));
    }

    public int length() {
        return sceneItems.size();
    }

    @Override
    protected void saveValue(JsonWriter writer) throws IOException {
        if (getSceneItem(uuid_master) != null)
            writer.name("uuid_master").value(uuid_master);

        writer.name("groupItems").beginArray();
        for (SceneItem c : sceneItems) {
            writer.beginObject();
            writer.name("uid").value(c.uuid);
            writer.name("name").value(c.command);
            writer.endObject();
        }
        writer.endArray();
    }

    private SceneItem getSceneItem(String uuid) {
        if (uuid == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid))
                return item;
        return null;
    }

    protected void loadValue(String name, JsonReader reader) throws IOException {
        switch (name) {
            case "uuid_master":
                updateMaster(reader.nextString());
                break;
            case "groupItems":
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    SceneItem item = new SceneItem();
                    while (reader.hasNext()) {
                        String nameSceneItem = reader.nextName();
                        assert nameSceneItem != null;
                        switch (nameSceneItem) {
                            case "name":
                                item.command = reader.nextInt();
                                break;
                            case "uid":
                                item.uuid = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                    if (item.uuid != null)
                        sceneItems.add(item);
                }
                reader.endArray();
                break;
            default:
                reader.skipValue();
                break;
        }
    }

    @Override
    public String getDescription() {
        Context context = App.instance;
        return isMasterSlave() ? context.getString(R.string.master_slave) : context.getString(R.string.scene);
    }

    public void setReachable(ReachabilityStates reachable) {
        this.reachable = reachable;
    }

    public ReachabilityStates reachableState() {
        return reachable;
    }

}
