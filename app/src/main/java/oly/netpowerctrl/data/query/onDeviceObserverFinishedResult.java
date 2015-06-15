package oly.netpowerctrl.data.query;

import java.util.List;

import oly.netpowerctrl.credentials.Credentials;

public interface onDeviceObserverFinishedResult {
    void onObserverJobFinished(List<Credentials> timeout_credentialses);
}
