package oly.netpowerctrl.executables;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;

/**
 * Created by david on 04.11.14.
 */
public class AdapterSource implements onServiceReady, onDataQueryCompleted {
    private final List<AdapterSourceInput> list = new ArrayList<>();
    protected boolean hideNotReachable = false;
    protected onChange onChangeListener = null;
    Executable ignoreUpdatesExecutable;
    private WeakReference<ExecutablesBaseAdapter> adapterWeakReference;
    private boolean automaticUpdatesEnabled = false;
    private WeakReference<AppData> appDataWeakReference = new WeakReference<>(null);

    public AdapterSource(AutoStartEnum autoStart) {
        if (autoStart == AutoStartEnum.AutoStartOnServiceReady) {
            PluginService.observersServiceReady.register(this);
        } else if (autoStart == AutoStartEnum.AutoStartAfterFirstQuery) {
            AppData.observersDataQueryCompleted.register(this);
        }
    }

    @Override
    public boolean onDataQueryFinished(AppData appData, boolean networkDevicesNotReachable) {
        start(true, appData);
        return false;
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        start(true, service.getAppData());
        return false;
    }

    @Override
    public void onServiceFinished(PluginService service) {
        for (AdapterSourceInput base : list)
            base.onFinish();
        appDataWeakReference = new WeakReference<>(null);
    }

    public void ignoreUpdates(Executable executable) {
        ignoreUpdatesExecutable = executable;
    }

    /**
     * Call this from your AdapterSourceInput if you changed data in the adapter.
     */
    void sourceChanged() {
        if (onChangeListener != null)
            onChangeListener.sourceChanged();
    }

    public AppData getAppData() {
        return appDataWeakReference.get();
    }

    public ExecutablesBaseAdapter getAdapter() {
        return adapterWeakReference.get();
    }

    final public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    final public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
        updateNow();
    }

    /**
     * If automatic updates are automaticUpdates, new values are automatically
     * synced with the target adapter.
     *
     * @param automaticUpdates Enable automatic updates
     * @param appData          A valid reference to AppData. Call this method in onServiceReady
     *                         to get a valid reference.
     */
    final public void start(boolean automaticUpdates, AppData appData) {
        this.appDataWeakReference = new WeakReference<>(appData);
        automaticUpdatesEnabled = automaticUpdates;

        for (AdapterSourceInput base : list)
            base.onStart(appData);
    }

    final public boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    final public void setTargetAdapter(ExecutablesBaseAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
        updateNow();
    }

    /**
     * Update the target adapter with new values immediately.
     * You do not need to call this method if automatic updates
     * are enabled.
     *
     * @see #start(boolean, oly.netpowerctrl.data.AppData)
     */
    final public void updateNow() {
        if (adapterWeakReference == null || appDataWeakReference.get() == null || list.isEmpty())
            return;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();

        adapter.markAllRemoved();

        for (AdapterSourceInput base : list)
            base.doUpdateNow(adapter);

        adapter.removeAllMarked();

        if (adapter.getItemCount() == 0)
            adapter.notifyDataSetChanged();

        if (onChangeListener != null)
            onChangeListener.sourceChanged();
    }

    public void add(AdapterSourceInput... inputs) {
        AppData appData = getAppData();
        for (AdapterSourceInput adapterSourceInput : inputs) {
            list.add(adapterSourceInput);
            adapterSourceInput.setAdapterSource(this);
            if (appData != null)
                adapterSourceInput.onStart(appData);
        }
    }

    public enum AutoStartEnum {
        NoAutoStart, AutoStartOnServiceReady, AutoStartAfterFirstQuery
    }

    public interface onChange {
        void sourceChanged();
    }
}
