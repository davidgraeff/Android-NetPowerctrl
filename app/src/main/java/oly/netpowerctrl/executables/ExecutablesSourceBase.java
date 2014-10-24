package oly.netpowerctrl.executables;

import java.lang.ref.WeakReference;

/**
 * Created by david on 07.07.14.
 */
public abstract class ExecutablesSourceBase {
    final static String TAG = "ExecutablesSourceBase";
    protected WeakReference<ExecutablesBaseAdapter> adapterWeakReference;
    protected boolean automaticUpdatesEnabled = false;

    protected boolean hideNotReachable = false;
    protected onChange onChangeListener = null;
    private ExecutablesSourceBase chained = null;

    public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    /**
     * Update the target adapter with new values immediately.
     * You do not need to call this method if automatic updates
     * are enabled.
     *
     * @see #setAutomaticUpdate(boolean)
     */
    public final void updateNow() {
        if (adapterWeakReference == null)
            return;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();

        adapter.markAllRemoved();
        fullUpdate(adapter);

        if (chained != null) {
            chained.fullUpdate(adapter);
            adapter.removeAllMarked();
        } else {
            adapter.removeAllMarked();
            adapter.notifyDataSetChanged();
            if (onChangeListener != null)
                onChangeListener.sourceChanged();
        }
    }

    protected void fullUpdate(ExecutablesBaseAdapter adapter) {
        if (chained != null)
            chained.fullUpdate(adapter);
    }

    /**
     * If automatic updates are enabled, new values are automatically
     * synced with the target adapter.
     *
     * @param enabled
     */
    public final void setAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (enabled)
            automaticUpdatesEnable();
        else
            automaticUpdatesDisable();

        if (chained != null) {
            chained.setAutomaticUpdate(enabled);
        }
    }

    public final void setTargetAdapter(ExecutablesBaseAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
        if (chained != null)
            chained.setTargetAdapter(adapter);
    }

    protected abstract void automaticUpdatesEnable();

    protected abstract void automaticUpdatesDisable();

    public final boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    public void onPause() {
        automaticUpdatesDisable();
        if (chained != null)
            chained.onPause();
    }

    public void onResume() {
        automaticUpdatesEnable();
        if (chained != null)
            chained.onResume();
    }

    public final void addChainItem(ExecutablesSourceBase item) {
        chained = item;
    }

    public interface onChange {
        void sourceChanged();
    }
}
