package oly.netpowerctrl.plugins;

interface INetPwrCtrlPluginResult {
	void intValue(int id, String name, int min, int max, int value);
	void booleanValue(int id, String name, boolean value);
	void action(int id, String name);
	void header(int id, String name);
	void finished();
}
