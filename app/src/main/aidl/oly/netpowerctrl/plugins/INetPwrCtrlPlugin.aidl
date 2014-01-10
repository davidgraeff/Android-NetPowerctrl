package oly.netpowerctrl.plugins;

import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;

interface INetPwrCtrlPlugin {
	void requestValues(INetPwrCtrlPluginResult cb);
	void updateIntValue(int id, int value, INetPwrCtrlPluginResult cb);
	void updateBooleanValue(int id, boolean value, INetPwrCtrlPluginResult cb);
	void executeAction(int id, INetPwrCtrlPluginResult cb);
}
