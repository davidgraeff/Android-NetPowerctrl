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

    public void restartDiscoveryThreads(Activity activity) {
    	for (DiscoveryThread thr: discoveryThreads)
    		thr.interrupt();
    	discoveryThreads.clear();
    	
    	
    	for (int port: DeviceQuery.getAllReceivePorts(this)) {
        	DiscoveryThread thr = new DiscoveryThread(port, activity);
        	thr.start();
        	discoveryThreads.add(thr);
        }
	}
	
}
