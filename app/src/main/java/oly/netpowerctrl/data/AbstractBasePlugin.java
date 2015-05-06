package oly.netpowerctrl.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.onNewIOConnection;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.network.onExecutionFinished;

/**
 * This interface defines a plugin
 */
public abstract class AbstractBasePlugin {
    protected final DataService dataService;
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

    ////////////// Request data and executeToggle //////////////
    abstract public void requestData();

    abstract public void requestData(@NonNull IOConnection ioConnection);

    abstract public boolean execute(@NonNull Executable executable, final int command, @Nullable onExecutionFinished callback);

    abstract public void addToTransaction(@NonNull Executable executable, final int command);

    abstract public void executeTransaction(@Nullable onExecutionFinished callback);

    abstract public void setTitle(@NonNull Executable executable, @NonNull final String new_name, @Nullable onNameChangeResult callback);

    ////////////// Auxiliary //////////////
    abstract public String getPluginID();

    abstract public String getLocalizedName();

    /**
     * Open external configuration website, app or whatever for the given device.
     *
     * @param credentials The device, identified by the credentials.
     */
    abstract public void openConfigurationPage(Credentials credentials);

    /**
     * Start editing an existing device by existing credentials (where configured==true) or edit
     * a new device by a not configured credentials object or null.
     */
    @Nullable
    abstract public Credentials createNewDefaultCredentials();

    abstract public boolean hasEditableCredentials();

    abstract public boolean isNewIOConnectionAllowed(Credentials credentials);

    /**
     * Provide credentials and then the plugin will call you back asynchronously with a new connection.
     * <p/>
     * For plugin developers:
     * If your plugin only provide one type of IOConnection you may immediately call callback.newIOConnection in your
     * implementation. Otherwise you may show a dialog to the user and make him select the type of connection to create.
     *
     * @param credentials Credentials that identify a device.
     * @param callback    The callback method with the new IOConnection.
     */
    abstract public void addNewIOConnection(@NonNull Credentials credentials, @NonNull onNewIOConnection callback);

    public abstract ReachabilityStates getReachableState(Executable executable);
}
