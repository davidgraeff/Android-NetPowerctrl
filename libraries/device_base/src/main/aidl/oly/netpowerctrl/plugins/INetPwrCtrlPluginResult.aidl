package oly.netpowerctrl.plugins;

oneway interface INetPwrCtrlPluginResult {
	void finished();

	//////// PARTITIAL Updates ////////
    // Update one connection.
    void deviceConnectionChanged(in String device_unique_id, in String device_connection_json);
    // Update one device port.
	void devicePortChanged      (in String device_unique_id, in String device_port_json);

	//////// FULL Update ////////
	void deviceChanged(in String device_json);
}
