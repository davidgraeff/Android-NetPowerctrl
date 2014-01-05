package oly.netpowerctrl.main;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceError;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceUpdate;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.anelservice.NetpowerctrlService;
import oly.netpowerctrl.anelservice.ServiceReady;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Application state: We keep track of Anel device states via
 * the listener service.
 */
public class NetpowerctrlApplication extends Application implements DeviceUpdate, DeviceError {
    public static NetpowerctrlApplication instance;
    private int mDiscoverServiceRefCount = 0;
    private NetpowerctrlService mDiscoverService;
    public ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public ArrayList<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();

    private ArrayList<DevicesUpdate> observersConfigured = new ArrayList<DevicesUpdate>();
    private ArrayList<DevicesUpdate> observersNew = new ArrayList<DevicesUpdate>();
    private ArrayList<ServiceReady> observersServiceReady = new ArrayList<ServiceReady>();

    private ArrayList<DeviceQuery> updateDeviceStateList = new ArrayList<DeviceQuery>();

    //! get a list of all send ports of all configured scenes plus the default send port
    public ArrayList<Integer> getAllSendPorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultSendPort(this));

        for (DeviceInfo di : configuredDevices)
            ports.add(di.SendPort);

        return new ArrayList<Integer>(ports);
    }

    //! get a list of all receive ports of all configured scenes plus the default receive port
    public ArrayList<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultReceivePort(this));

        for (DeviceInfo di : configuredDevices)
            ports.add(di.ReceivePort);

        return new ArrayList<Integer>(ports);
    }

    @SuppressWarnings("unused")
    public boolean registerConfiguredObserver(DevicesUpdate o) {
        if (!observersConfigured.contains(o)) {
            observersConfigured.add(o);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterConfiguredObserver(DevicesUpdate o) {
        observersConfigured.remove(o);
    }

    private void notifyConfiguredObservers() {
        for (DevicesUpdate o : observersConfigured)
            o.onDevicesUpdated();

    }

    @SuppressWarnings("unused")
    public boolean registerNewDeviceObserver(DevicesUpdate o) {
        if (!observersNew.contains(o)) {
            observersNew.add(o);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterNewDeviceObserver(DevicesUpdate o) {
        observersNew.remove(o);
    }

    private void notifyNewDeviceObservers() {
        for (DevicesUpdate o : observersNew)
            o.onDevicesUpdated();
    }

    @SuppressWarnings("unused")
    public boolean registerServiceReadyObserver(ServiceReady o) {
        if (!observersServiceReady.contains(o)) {
            observersServiceReady.add(o);
            if (mDiscoverService != null)
                o.onServiceReady(mDiscoverService);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterServiceReadyObserver(ServiceReady o) {
        observersServiceReady.remove(o);
    }

    private void notifyServiceReady() {
        for (ServiceReady o : observersServiceReady)
            o.onServiceReady(mDiscoverService);
    }


    public void removeUpdateDeviceState(DeviceQuery o) {
        updateDeviceStateList.remove(o);
    }

    @SuppressWarnings("unused")
    public void updateDeviceState(DeviceUpdateStateOrTimeout target, Collection<DeviceInfo> devices_to_observe) {
        // Create device query object (init timeout, send query packet)
        updateDeviceStateList.add(new DeviceQuery(getApplicationContext(), target, devices_to_observe));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        reloadConfiguredDevices();
    }

    public void startListener() {
        if (mDiscoverServiceRefCount == 0) {
            Intent intent = new Intent(this, NetpowerctrlService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        ++mDiscoverServiceRefCount;
    }

    public void restartListener() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverService.restartDiscoveryThreads();
        }
    }

    public void stopListener() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0) {
            unbindService(mConnection);
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
            if (mDiscoverServiceRefCount == 0)
                mDiscoverServiceRefCount = 1;
            instance.notifyServiceReady();
            DeviceQuery.sendBroadcastQuery(instance);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDiscoverServiceRefCount = 0;
        }
    };

    public void reloadConfiguredDevices() {
        List<DeviceInfo> newEntries = SharedPrefs.ReadConfiguredDevices(this);
        // This is somehow a more complicated way of alDevices := newEntries
        // because with an assignment we would lose the outlet states which are not stored in the SharedPref
        // and only with the next UDP update those states are restored. This
        // induces a flicker where we can first observe all outlets set to off and
        // within a fraction of a second the actual values are applied.
        // Therefore we update by hand without touching outlet states.
        Iterator<DeviceInfo> oldEntriesIt = configuredDevices.iterator();
        while (oldEntriesIt.hasNext()) {
            // remove scenes not existing anymore
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
        // add new scenes
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

        // Already in configured scenes?
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

        // Remove from new scenes
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
                        target_oi.setDescriptionByDevice(source_oi.getDescription());
                        break;
                    }
                }
            }

            // notify all observers
            notifyConfiguredObservers();

            //
            for (DeviceQuery o : updateDeviceStateList)
                o.notifyAndRemove(target);

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
        String error = getResources().getString(R.string.error_packet_received) + ": " + desc;
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

    public void saveConfiguredDevices() {
        SharedPrefs.SaveConfiguredDevices(configuredDevices, this);
    }
}
