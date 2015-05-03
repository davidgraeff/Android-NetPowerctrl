package oly.netpowerctrl.devices;

import java.util.List;

public interface onDeviceObserverFinishedResult {
    void onObserverJobFinished(List<Credentials> timeout_credentialses);
}
