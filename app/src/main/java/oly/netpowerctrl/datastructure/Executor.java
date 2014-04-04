package oly.netpowerctrl.datastructure;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.anel.AnelExecutor;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Execution arbiter. Distribute commands/queries to ether the AnelExecutor or to the
 * plugin controller. Should not be necessary if Anel is implemented as a Plugin.
 */
public class Executor {

    public static class PortAndCommand {
        public DevicePort port;
        public Integer command;
    }

    public static void execute(Scene scene, ExecutionFinished callback) {
        List<PortAndCommand> command_list_anel = new ArrayList<PortAndCommand>();
        List<PortAndCommand> command_list_plugin = new ArrayList<PortAndCommand>();
        for (Scene.SceneItem item : scene.sceneItems) {
            PortAndCommand p = new PortAndCommand();
            p.command = item.command;
            p.port = NetpowerctrlApplication.getDataController().findDevicePort(item.uuid);
            if (p.port == null)
                continue;
            if (p.port.device.deviceType == DeviceInfo.DeviceType.AnelDevice)
                command_list_anel.add(p);
            else if (p.port.device.deviceType == DeviceInfo.DeviceType.PluginDevice)
                command_list_plugin.add(p);
        }
        AnelExecutor.execute(command_list_anel, callback);
        NetpowerctrlApplication.getPluginController().execute(command_list_plugin, callback);
    }

    public static void execute(DevicePort port, int command, ExecutionFinished callback) {
        if (port.device.deviceType == DeviceInfo.DeviceType.AnelDevice)
            AnelExecutor.execute(port, command, callback);
        else if (port.device.deviceType == DeviceInfo.DeviceType.PluginDevice)
            NetpowerctrlApplication.getPluginController().execute(port, command, callback);
    }


    public static void sendQuery(DeviceInfo di) {
        if (di.deviceType == DeviceInfo.DeviceType.AnelDevice)
            AnelExecutor.sendQuery(di);
        else if (NetpowerctrlApplication.getPluginController() != null)
            NetpowerctrlApplication.getPluginController().sendQuery(di);
    }

    public static void sendBroadcastQuery() {
        AnelExecutor.sendBroadcastQuery();
        if (NetpowerctrlApplication.getPluginController() != null)
            NetpowerctrlApplication.getPluginController().sendBroadcastQuery();
    }

}
