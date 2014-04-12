package oly.netpowerctrl.plugins;

import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;

interface INetPwrCtrlPlugin {
	void init(INetPwrCtrlPluginResult cb);
	void finish();
    void requestValues();
    void updateIntValue(int id, int value);
    void updateBooleanValue(int id, boolean value);
    void executeAction(int id);
    void rename(int id, String new_name);
}
