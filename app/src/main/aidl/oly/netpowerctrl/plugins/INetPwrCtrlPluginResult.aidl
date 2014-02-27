package oly.netpowerctrl.plugins;

interface INetPwrCtrlPluginResult {
    void pluginState(int state);
	void intValue(int id, String name, int min, int max, int value);
	void booleanValue(int id, String name, boolean value);
	void action(int id, String name);
	void header(String name);
	void finished();

}
