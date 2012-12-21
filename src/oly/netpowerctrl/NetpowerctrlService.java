package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class NetpowerctrlService extends Service {

	public static String BROADCAST_PORTS_CHANGED = "com.nittka.netpowerctrl.DEVICE_DISCOVERED";
	List<DiscoveryThread> discoveryThreads;
	
	@Override
	public void onCreate() {
		//android.os.Debug.waitForDebugger();
        discoveryThreads = new ArrayList<DiscoveryThread>();
    	IntentFilter itf= new IntentFilter(DiscoveryThread.BROADCAST_RESTART_DISCOVERY);
        LocalBroadcastManager.getInstance(this).registerReceiver(onConfigChanged, itf);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// not needed, we use broadcasts
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    startDiscoveryThreads();
	    return START_STICKY;
	}
	
    public void startDiscoveryThreads() {
    	// only start if not yet running
    	if (discoveryThreads.size() == 0) {
	    	for (int port: DeviceQuery.getAllReceivePorts(this)) {
	        	DiscoveryThread thr = new DiscoveryThread(port, this);
	        	thr.start();
	        	discoveryThreads.add(thr);
	        }
    	}
    	// give the threads a chance to start
    	try {Thread.sleep(100);} catch (InterruptedException e) {}
	}
	
    public void restartDiscoveryThreads() {
    	for (DiscoveryThread thr: discoveryThreads)
    		thr.interrupt();
    	discoveryThreads.clear();
    	// socket needs minimal time to really go away
    	try {Thread.sleep(100);} catch (InterruptedException e) {}
    	startDiscoveryThreads();
	}
	
	private BroadcastReceiver onConfigChanged= new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
			restartDiscoveryThreads();
		}
	};
    
}
