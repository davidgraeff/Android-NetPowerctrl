package oly.netpowerctrl.plugins;

interface INetPwrCtrlPluginResult {
	void finished();
    void stateChanged(String state, boolean isError);
	void devicePortsChanged(in List<String> devicePorts_json);
	void deviceChanged(in String device_json);
}
