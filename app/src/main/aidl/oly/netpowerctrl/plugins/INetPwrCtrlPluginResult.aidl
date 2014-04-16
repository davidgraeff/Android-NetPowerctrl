package oly.netpowerctrl.plugins;
import java.util.List;

interface INetPwrCtrlPluginResult {
    void initDone(in List<String> states, int success_state);
    void pluginState(int state);
	void intValue(int id, String name, int min, int max, int value);
	void booleanValue(int id, String name, boolean value);
	void action(int id, int groupID, String name);
	void finished();

}
