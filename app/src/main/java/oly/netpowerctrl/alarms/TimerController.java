package oly.netpowerctrl.alarms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Control all configured alarms
 */
public class TimerController {
    private List<Alarm> alarms = new ArrayList<>();
    private final WeakHashMap<IAlarmsUpdated, Boolean> observers = new WeakHashMap<>();

    public interface IAlarmsUpdated {
        void alarmsUpdated(boolean addedOrRemoved);
    }

    public int getCount() {
        return alarms.size();
    }

    public Alarm getItem(int i) {
        return alarms.get(i);
    }

    @SuppressWarnings("unused")
    public void registerObserver(IAlarmsUpdated o) {
        if (!observers.containsKey(o)) {
            observers.put(o, true);
        }
    }

    @SuppressWarnings("unused")
    public void unregisterObserver(IAlarmsUpdated o) {
        observers.remove(o);
    }

    @SuppressWarnings("unused")
    private void notifyObservers(boolean addedOrRemoved) {
        Iterator<IAlarmsUpdated> i = observers.keySet().iterator();
        while (i.hasNext())
            i.next().alarmsUpdated(addedOrRemoved);
    }
}
