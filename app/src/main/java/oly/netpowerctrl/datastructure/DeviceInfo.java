package oly.netpowerctrl.datastructure;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;

// this class holds all the info about one device
public class DeviceInfo implements Parcelable {

    public UUID uuid;

    public String DeviceName; // name of the device as reported by UDP or configured by the user
    public String HostName;   // the hostname or ip address used to reach the device
    public String MacAddress; // the mac address as sent from the device

    public String UserName;
    public String Password;

    public boolean DefaultPorts;
    public int SendPort;
    public int ReceivePort;
    public int HttpPort;

    public List<OutletInfo> Outlets;
    public List<OutletInfo> IOs;
    public String Temperature;
    public String FirmwareVersion;
    public boolean reachable;

    private static String uuidToString(UUID uuid) {
        return uuid.toString().replace(":", "-");
    }

    public OutletInfo findOutlet(int outletNumber) {
        for (OutletInfo oi : Outlets) {
            if (oi.OutletNumber == outletNumber) {
                return oi;
            }
        }
        return null;
    }

    private DeviceInfo() {
        uuid = UUID.randomUUID();
        DeviceName = "";
        HostName = "";
        MacAddress = "";
        UserName = "";
        Password = "";
        DefaultPorts = true;
        SendPort = -1;
        ReceivePort = -1;
        HttpPort = 80;
        Temperature = "";
        FirmwareVersion = "";
        reachable = false;
        Outlets = new ArrayList<OutletInfo>();
        IOs = new ArrayList<OutletInfo>();
    }

    public DeviceInfo(Context cx) {
        this();
        DeviceName = cx.getResources().getString(R.string.default_device_name);
        SendPort = SharedPrefs.getDefaultSendPort(cx);
        ReceivePort = SharedPrefs.getDefaultReceivePort(cx);
    }

    public DeviceInfo(DeviceInfo other) {
        uuid = UUID.randomUUID();
        DeviceName = other.DeviceName;
        HostName = other.HostName;
        MacAddress = other.MacAddress;
        UserName = other.UserName;
        Password = other.Password;
        DefaultPorts = other.DefaultPorts;
        SendPort = other.SendPort;
        ReceivePort = other.ReceivePort;
        HttpPort = other.HttpPort;
        Temperature = other.Temperature;
        FirmwareVersion = other.FirmwareVersion;
        Outlets = new ArrayList<OutletInfo>();
        IOs = new ArrayList<OutletInfo>();
        for (OutletInfo oi : other.Outlets)
            Outlets.add(new OutletInfo(oi));
        for (OutletInfo oi : other.IOs)
            IOs.add(new OutletInfo(oi));
    }

    public void copyFreshValues(DeviceInfo di) {
        for (OutletInfo source_oi : di.Outlets) {
            for (OutletInfo target_oi : Outlets) {
                if (target_oi.OutletNumber == source_oi.OutletNumber) {
                    target_oi.State = source_oi.State;
                    target_oi.Disabled = source_oi.Disabled;
                    target_oi.setDescriptionByDevice(source_oi.getDescription());
                    break;
                }
            }
        }
        for (OutletInfo source_oi : di.IOs) {
            for (OutletInfo target_oi : IOs) {
                if (target_oi.OutletNumber == source_oi.OutletNumber) {
                    target_oi.State = source_oi.State;
                    target_oi.Disabled = source_oi.Disabled;
                    target_oi.setDescriptionByDevice(source_oi.getDescription());
                    break;
                }
            }
        }

        HostName = di.HostName;
        HttpPort = di.HttpPort;
        Temperature = di.Temperature;
        FirmwareVersion = di.FirmwareVersion;
    }

    /**
     * Return true if both DeviceInfo objects refer to the same device.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equalsFunctional(DeviceInfo other) {
        return HostName.equals(other.HostName) && ReceivePort == other.ReceivePort &&
                Outlets.size() == other.Outlets.size();
    }

    /**
     * Return true if this and the other DeviceInfo are the same configured DeviceInfo.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equals(DeviceInfo other) {
        return uuid.equals(other.uuid);
    }

    @SuppressWarnings("unused")
    public boolean equals(UUID uuid) {
        return uuid.equals(uuid);
    }

    public String getID() {
        return uuidToString(uuid);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(uuid.toString());
        destination.writeString(DeviceName);
        destination.writeString(HostName);
        destination.writeString(MacAddress);
        destination.writeString(UserName);
        destination.writeString(Password);
        destination.writeString(Temperature);
        destination.writeString(FirmwareVersion);
        destination.writeInt(DefaultPorts ? 1 : 0);
        destination.writeInt(SendPort);
        destination.writeInt(ReceivePort);
        destination.writeInt(HttpPort);
        destination.writeTypedList(Outlets);
        destination.writeTypedList(IOs);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<DeviceInfo> CREATOR = new Parcelable.Creator<DeviceInfo>() {
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private DeviceInfo(Parcel in) {
        this();
        uuid = UUID.fromString(in.readString());
        DeviceName = in.readString();
        HostName = in.readString();
        MacAddress = in.readString();
        UserName = in.readString();
        Password = in.readString();
        Temperature = in.readString();
        FirmwareVersion = in.readString();
        DefaultPorts = in.readInt() != 0;
        SendPort = in.readInt();
        ReceivePort = in.readInt();
        HttpPort = in.readInt();
        in.readTypedList(Outlets, OutletInfo.CREATOR);
        in.readTypedList(IOs, OutletInfo.CREATOR);
    }

    public void updateByDeviceCommand(DeviceCommand c) {
        for (OutletInfo oi : Outlets) {
            oi.State = c.getIsOn(oi.OutletNumber);
        }
    }

    public static DeviceInfo fromJSON(JsonReader reader) throws IOException {
        reader.beginObject();
        DeviceInfo di = new DeviceInfo();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("uuid")) {
                di.uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("DeviceName")) {
                di.DeviceName = reader.nextString();
            } else if (name.equals("HostName")) {
                di.HostName = reader.nextString();
            } else if (name.equals("MacAddress")) {
                di.MacAddress = reader.nextString();
            } else if (name.equals("UserName")) {
                di.UserName = reader.nextString();
            } else if (name.equals("Password")) {
                di.Password = reader.nextString();
            } else if (name.equals("Temperature")) {
                di.Temperature = reader.nextString();
            } else if (name.equals("FirmwareVersion")) {
                di.FirmwareVersion = reader.nextString();
            } else if (name.equals("DefaultPorts")) {
                di.DefaultPorts = reader.nextBoolean();
            } else if (name.equals("SendPort")) {
                di.SendPort = reader.nextInt();
            } else if (name.equals("ReceivePort")) {
                di.ReceivePort = reader.nextInt();
            } else if (name.equals("HttpPort")) {
                di.HttpPort = reader.nextInt();
            } else if (name.equals("IOs")) {
                di.IOs.clear();
                reader.beginArray();
                while (reader.hasNext()) {
                    di.IOs.add(OutletInfo.fromJSON(reader, di));
                }
                reader.endArray();
            } else if (name.equals("Outlets")) {
                di.Outlets.clear();
                reader.beginArray();
                while (reader.hasNext()) {
                    di.Outlets.add(OutletInfo.fromJSON(reader, di));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        return di;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("uuid").value(uuid.toString());
        writer.name("DeviceName").value(DeviceName);
        writer.name("HostName").value(HostName);
        writer.name("MacAddress").value(MacAddress);
        writer.name("UserName").value(UserName);
        writer.name("Password").value(Password);
        writer.name("Temperature").value(Temperature);
        writer.name("FirmwareVersion").value(FirmwareVersion);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("SendPort").value(SendPort);
        writer.name("ReceivePort").value(ReceivePort);
        writer.name("HttpPort").value(HttpPort);
        writer.name("Outlets").beginArray();
        assert Outlets != null;
        for (OutletInfo oi : Outlets) {
            oi.toJSON(writer);
        }
        writer.endArray();
        writer.name("IOs").beginArray();
        assert IOs != null;
        for (OutletInfo oi : IOs) {
            oi.toJSON(writer);
        }
        writer.endArray();
        writer.endObject();
    }
}
