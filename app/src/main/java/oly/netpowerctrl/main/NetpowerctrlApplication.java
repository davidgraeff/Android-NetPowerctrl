package oly.netpowerctrl.main;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceError;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceUpdated;
import oly.netpowerctrl.anelservice.NetpowerctrlService;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Application state: We keep track of Anel device states via
 * the listener service.
 */
public class NetpowerctrlApplication extends Application implements DeviceUpdated, DeviceError {
    public static NetpowerctrlApplication instance;
    private boolean mDiscoverServiceStarted = false;
    private NetpowerctrlService mDiscoverService;
    public ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public ArrayList<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();

    private ArrayList<DeviceUpdated> observersConfigured = new ArrayList<DeviceUpdated>();
    private ArrayList<DeviceUpdated> observersNew = new ArrayList<DeviceUpdated>();

    @SuppressWarnings("unused")
    public void registerConfiguredObserver(DeviceUpdated o) {
        if (!observersConfigured.contains(o))
            observersConfigured.add(o);
    }

    @SuppressWarnings("unused")
    public void unregisterConfiguredObserver(DeviceUpdated o) {
        observersConfigured.remove(o);
    }

    private void notifyConfiguredObservers() {
        for (DeviceUpdated o : observersConfigured)
            o.onDeviceUpdated(null);
    }

    @SuppressWarnings("unused")
    public void registerNewDeviceObserver(DeviceUpdated o) {
        if (!observersNew.contains(o))
            observersNew.add(o);
    }

    @SuppressWarnings("unused")
    public void unregisterNewDeviceObserver(DeviceUpdated o) {
        observersNew.remove(o);
    }

    private void notifyNewDeviceObservers() {
        for (DeviceUpdated o : observersNew)
            o.onDeviceUpdated(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        reloadConfiguredDevices();
    }

    public void startListener() {
        if (!mDiscoverServiceStarted) {
            Intent intent = new Intent(this, NetpowerctrlService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void restartListening() {
        startListener();
        mDiscoverService.restartDiscoveryThreads();
    }

    public void stopListener() {
        if (mDiscoverServiceStarted) {
            unbindService(mConnection);
            mDiscoverServiceStarted = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NetpowerctrlService.LocalBinder binder = (NetpowerctrlService.LocalBinder) service;
            mDiscoverService = binder.getService();
            mDiscoverService.registerDeviceErrorObserver(instance);
            mDiscoverService.registerDeviceUpdateObserver(instance);
            mDiscoverServiceStarted = true;
            DeviceQuery.sendBroadcastQuery(instance);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDiscoverServiceStarted = false;
        }
    };

    public void reloadConfiguredDevices() {
        ArrayList<DeviceInfo> newEntries = SharedPrefs.ReadConfiguredDevices(this);
        // This is somehow a more complicated way of alDevices := newEntries
        // because with an assignment we would lose the outlet states which are not stored in the SharedPref
        // and only with the next UDP update those states are restored. This
        // induces a flicker where we can first observe all outlets set to off and
        // within a fraction of a second the actual values are applied.
        // Therefore we update by hand without touching outlet states.
        Iterator<DeviceInfo> oldEntriesIt = configuredDevices.iterator();
        while (oldEntriesIt.hasNext()) {
            // remove devices not existing anymore
            DeviceInfo old_di = oldEntriesIt.next();
            DeviceInfo new_di = null;
            for (DeviceInfo it_di : newEntries) {
                if (it_di.MacAddress.equals(old_di.MacAddress)) {
                    new_di = it_di;
                    break;
                }
            }
            if (new_di == null) {
                oldEntriesIt.remove();
            } else if (old_di.Outlets.size() != new_di.Outlets.size()) {
                // Number of outlets have changed. This is a reason to forget about
                // outlet states and replace the old device with the new one.
                oldEntriesIt.remove();
            }
        }
        // add new devices
        for (DeviceInfo new_di : newEntries) {
            DeviceInfo old_di = null;
            for (int i = 0; i < configuredDevices.size(); ++i) {
                if (configuredDevices.get(i).MacAddress.equals(new_di.MacAddress)) {
                    old_di = newEntries.get(i);
                    break;
                }
            }
            if (old_di == null) {
                configuredDevices.add(new_di);
            }
        }
        notifyConfiguredObservers();
    }

    public void addToConfiguredDevices(DeviceInfo current_device) {
        // remove it's disabled outlets
        for (Iterator<OutletInfo> i = current_device.Outlets.iterator(); i.hasNext(); )
            if (i.next().Disabled)
                i.remove();

        // Already in configured devices?
        for (int i = configuredDevices.size() - 1; i >= 0; --i) {
            if (current_device.MacAddress.equals(configuredDevices.get(i).MacAddress)) {
                configuredDevices.set(i, current_device);
                DeviceQuery.sendBroadcastQuery(this);
                SaveConfiguredDevices();
                return;
            }
        }

        DeviceInfo new_device = new DeviceInfo(current_device);
        new_device.setConfigured(true);
        configuredDevices.add(new_device);
        DeviceQuery.sendBroadcastQuery(this);
        SaveConfiguredDevices();

        // Remove from new devices
        for (int i = 0; i < newDevices.size(); ++i)
            if (newDevices.get(i).MacAddress.equals(current_device.MacAddress)) {
                newDevices.remove(i);
                notifyNewDeviceObservers();
                break;
            }
    }

    private void SaveConfiguredDevices() {
        SharedPrefs.SaveConfiguredDevices(configuredDevices, this);
        notifyConfiguredObservers();
    }

    public void deleteAllConfiguredDevices() {
        configuredDevices.clear();
        SaveConfiguredDevices();
    }

    public void deleteConfiguredDevice(int position) {
        configuredDevices.remove(position);
        SaveConfiguredDevices();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo device_info) {
        // if it matches a configured device, update it's outlet states
        for (DeviceInfo target : configuredDevices) {
            if (!device_info.MacAddress.equals(target.MacAddress))
                continue;
            for (OutletInfo source_oi : device_info.Outlets) {
                for (OutletInfo target_oi : target.Outlets) {
                    if (target_oi.OutletNumber == source_oi.OutletNumber) {
                        target_oi.State = source_oi.State;
                        target_oi.Disabled = source_oi.Disabled;
                        break;
                    }
                }
            }

            notifyConfiguredObservers();
            return;
        }

        // Device is a new one, set configured to false
        device_info.setConfigured(false);
        // Do we have this new device already in the list?
        for (DeviceInfo target : newDevices) {
            if (device_info.MacAddress.equals(target.MacAddress))
                return;
        }
        // No: Add device to new_device list
        newDevices.add(device_info);
        notifyNewDeviceObservers();
    }

    @Override
    public void onDeviceError(String deviceName, String errMessage) {
        // error packet received
        String desc;
        if (errMessage.trim().equals("NoPass"))
            desc = getResources().getString(R.string.error_nopass);
        else
            desc = errMessage;
        String error = getResources().getString(R.string.error_packet_received) + desc;
        ShowToast.FromOtherThread(this, error);
    }

    public NetpowerctrlService getService() {
        return mDiscoverService;
    }

    public DeviceInfo findDevice(String mac_address) {
        for (DeviceInfo di : configuredDevices) {
            if (di.MacAddress.equals(mac_address)) {
                return di;
            }
        }
        return null;
    }


    public OutletInfo findOutlet(String mac_address, int outletNumber) {
        for (DeviceInfo di : configuredDevices) {
            if (di.MacAddress.equals(mac_address)) {
                for (OutletInfo oi : di.Outlets) {
                    if (oi.OutletNumber == outletNumber) {
                        return oi;
                    }
                }
                return null;
            }
        }
        return null;
    }
}
