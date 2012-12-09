package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;

public class AppMain extends Application {

	List<DiscoveryThread> discoveryThreads;
	
	@Override
	public void onCreate() {
        discoveryThreads = new ArrayList<DiscoveryThread>();
	}

    public void startDiscoveryThreads(Activity activity) {
    	for (int port: DeviceQuery.getAllReceivePorts(this)) {
        	DiscoveryThread thr = new DiscoveryThread(port, activity);
        	thr.start();
        	discoveryThreads.add(thr);
        }
	}
	
    public void restartDiscoveryThreads(Activity activity) {
    	for (DiscoveryThread thr: discoveryThreads)
    		thr.interrupt();
    	discoveryThreads.clear();

    	startDiscoveryThreads(activity);
	}
	
    public void maybeStartDiscoveryThreads(Activity activity) {
    	// only start if not yet running
    	if (discoveryThreads.size() == 0)
    		startDiscoveryThreads(activity);
	}
	
}
