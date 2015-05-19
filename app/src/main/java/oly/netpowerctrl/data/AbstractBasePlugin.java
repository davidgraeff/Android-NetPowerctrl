package oly.netpowerctrl.data;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.network.onExecutionFinished;

/**
 * This interface defines a plugin
 */
public abstract class AbstractBasePlugin {
    protected final DataService dataService;
    protected final List<ExecutableAndCommand> command_list = new ArrayList<>();
    protected AbstractBasePlugin(DataService dataService) {
        this.dataService = dataService;
    }

    public DataService getDataService() {
        return dataService;
    }

    ////////////// Life cycle //////////////
    abstract public void onDestroy();

    abstract public void onStart(Context context);

    abstract public boolean isStarted();

    ////////////// Request data and execute //////////////
    abstract public void requestData();

    abstract public void requestData(@NonNull IOConnection ioConnection);

    abstract public boolean execute(@NonNull Executable executable, final int command, @Nullable onExecutionFinished callback);

    public void addToTransaction(@NonNull Executable port, int command) {
        command_list.add(new ExecutableAndCommand(port, command));
    }

    public void executeTransaction(@Nullable onExecutionFinished callback) {
        if (callback != null)
            callback.addExpected(command_list.size());
        for (ExecutableAndCommand executableAndCommand : command_list) {
            execute(executableAndCommand.executable, executableAndCommand.command, callback);
        }
        command_list.clear();
    }

    abstract public void setTitle(@NonNull Executable executable, @NonNull final String new_name, @Nullable onNameChangeResult callback);

    ////////////// Auxiliary //////////////
    abstract public String getPluginID();

    abstract public String getLocalizedName();

    /**
     * Open external configuration website, app or whatever for the given device.
     *
     * @param credentials The device, identified by the credentials.
     */
    public void openConfigurationPage(Credentials credentials) {
    }

    /**
     * Start editing an existing device by existing credentials (where configured==true) or edit
     * a new device by a not configured credentials object or null.
     */
    @Nullable
    abstract public Credentials createNewDefaultCredentials();

    public abstract boolean isNewIOConnectionAllowed(Credentials credentials);

    /**
     * Provide credentials and then the plugin will call you back asynchronously with a new connection.
     * <p/>
     * For plugin developers:
     * If your plugin only provide one type of IOConnection you may immediately call callback.newIOConnection in your
     * implementation. Otherwise you may show a dialog to the user and make him select the type of connection to create.
     *
     * @param credentials Credentials that identify a device.
     * @param activity The activity on which dialogs etc can be shown to select/create a connection.
     */
    public void addNewIOConnection(@NonNull Credentials credentials, @NonNull Activity activity) {
    }

    public abstract boolean supportProperty(Properties property);

    public enum Properties {
        RemoteRename, EditableUsername, EditablePassword, ManuallyAddDevice
    }
}
