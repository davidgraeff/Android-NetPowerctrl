package oly.netpowerctrl.plugins;

import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;

interface INetPwrCtrlPlugin {
    void init(INetPwrCtrlPluginResult cb);
    void finish();
	void requestDevices(long with_update_time_newer_than);
	void execute(in String devicePort_json, in int command);
	void rename(in String devicePort_json, in String new_name);
}
