package oly.netpowerctrl.datastructure;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.anel.AnelExecutor;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Execution
 */
public class Executor {

    public static class PortAndCommand {
        public DevicePort port;
        public Integer command;
    }

    public static void execute(Scene scene) {
        List<PortAndCommand> command_list = new ArrayList<PortAndCommand>();
        for (Scene.SceneItem item : scene.sceneItems) {
            PortAndCommand p = new PortAndCommand();
            p.command = item.command;
            p.port = NetpowerctrlApplication.instance.getDataController().findDevicePort(item.uuid);
            if (p.port == null)
                continue;
            command_list.add(p);
        }
        AnelExecutor.execute(command_list);
    }

    public static void execute(DevicePort port, int command) {
        if (port.device.deviceType == DeviceInfo.DeviceType.AnelDevice)
            AnelExecutor.execute(port, command);
        else if (port.device.deviceType == DeviceInfo.DeviceType.PluginDevice)
            NetpowerctrlApplication.instance.getPluginController().execute(port, command);
    }


    public static void sendQuery(DeviceInfo di) {
        if (di.deviceType == DeviceInfo.DeviceType.AnelDevice)
            AnelExecutor.sendQuery(di);
        else if (NetpowerctrlApplication.instance.getPluginController() != null)
            NetpowerctrlApplication.instance.getPluginController().sendQuery(di);
    }

    public static void sendBroadcastQuery() {
        AnelExecutor.sendBroadcastQuery();
        if (NetpowerctrlApplication.instance.getPluginController() != null)
            NetpowerctrlApplication.instance.getPluginController().sendBroadcastQuery();
    }

}
