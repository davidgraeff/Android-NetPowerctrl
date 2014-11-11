package oly.netpowerctrl.executables;

import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Created by david on 07.07.14.
 */
public abstract class ExecutablesSourceBase {
    final static String TAG = "ExecutablesSourceBase";
    protected WeakReference<ExecutablesBaseAdapter> adapterWeakReference;
    protected boolean automaticUpdatesEnabled = false;

    protected boolean hideNotReachable = false;
    protected onChange onChangeListener = null;
    private ExecutablesSourceChain chained = null;

    public ExecutablesSourceBase(@Nullable ExecutablesSourceChain executablesSourceChain) {
        this.chained = executablesSourceChain;
        if (chained != null)
            chained.add(this);
    }

    final public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    final public void setHideNotReachable(boolean hideNotReachable) {
        if (chained != null)
            chained.setHideNotReachable(hideNotReachable);
        else
            doSetHideNotReachable(hideNotReachable);
    }

    final public void doSetHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    final public int countIfGroup(UUID uuid) {
        if (chained != null)
            return chained.doCountIfGroup(uuid);
        else
            return doCountIfGroup(uuid);
    }

    public abstract int doCountIfGroup(UUID uuid);

    /**
     * Update the target adapter with new values immediately.
     * You do not need to call this method if automatic updates
     * are enabled.
     *
     * @see #setAutomaticUpdate(boolean)
     */
    final public void updateNow() {
        if (adapterWeakReference == null)
            return;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();

        adapter.markAllRemoved();

        if (chained != null) {
            chained.fullUpdate(adapter);
        } else {
            fullUpdate(adapter);
        }

        adapter.removeAllMarked();

        if (adapter.getItemCount() == 0)
            adapter.notifyDataSetChanged();

        if (onChangeListener != null)
            onChangeListener.sourceChanged();
    }

    abstract protected void fullUpdate(ExecutablesBaseAdapter adapter);

    /**
     * If automatic updates are enabled, new values are automatically
     * synced with the target adapter.
     *
     * @param enabled
     */
    final public void setAutomaticUpdate(boolean enabled) {
        if (chained != null)
            chained.setAutomaticUpdate(enabled);
        else
            applyAutomaticUpdate(enabled);
    }

    final public void applyAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (enabled)
            automaticUpdatesEnable();
        else
            automaticUpdatesDisable();

    }

    final public void setTargetAdapter(ExecutablesBaseAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
    }

    protected abstract void automaticUpdatesEnable();

    protected abstract void automaticUpdatesDisable();

    final public boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    final public void onPause() {
        automaticUpdatesDisable();
    }

    final public void onResume() {
        automaticUpdatesEnable();
    }

    public interface onChange {
        void sourceChanged();
    }
}
