package oly.netpowerctrl.device_ports;

/**
 * Created by david on 07.07.14.
 */
public interface DevicePortSourceInterface {
    /**
     * Update the target adapter with new values immediately.
     * You do not need to call this method if automatic updates
     * are enabled.
     *
     * @see #setAutomaticUpdate(boolean)
     */
    void updateNow();

    /**
     * If automatic updates are enabled, new values are automatically
     * synced with the target adapter.
     *
     * @param enabled
     */
    void setAutomaticUpdate(boolean enabled);

    void setTargetAdapter(DevicePortsBaseAdapter adapter);

    boolean isAutomaticUpdateEnabled();

    void addChainItem(DevicePortSourceInterface item);
}
