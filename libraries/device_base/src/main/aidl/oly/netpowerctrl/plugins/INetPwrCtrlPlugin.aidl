package oly.netpowerctrl.plugins;

import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;

oneway interface INetPwrCtrlPlugin {
    // Initialize the remote plugin (and provide a callback interface)
    void init(INetPwrCtrlPluginResult cb, in int api_version);

    // The main app will shutdown now, share that info with the remote plugin.
    void shutdown();

    /**
     * The main app wants to know about all devices of the plugin.
     * Plugin: Collect all devices and send those to the main app via the deviceChanged() method.
     */
	void requestData();

	// The main app want to know about one specific device only.
	// Send the corresponding device to the main app via the deviceChanged() method.
	void requestDataByConnection(in String device_connection_json);

	// Execute the command of the given devicePort and device.
	void execute(in String device_unique_id, in String devicePort_json, in int command);

	// Rename the given devicePort of the given device.
	void rename (in String device_unique_id, in String devicePort_json, in String new_name);
}
