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

    public boolean Configured;

    public List<OutletInfo> Outlets;

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
        Configured = false;
        Outlets = new ArrayList<OutletInfo>();
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
        Configured = other.Configured;
        Outlets = new ArrayList<OutletInfo>();
        for (OutletInfo oi : other.Outlets)
            Outlets.add(new OutletInfo(oi));
    }

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
        destination.writeInt(DefaultPorts ? 1 : 0);
        destination.writeInt(SendPort);
        destination.writeInt(ReceivePort);
        destination.writeTypedList(Outlets);
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
        DefaultPorts = in.readInt() != 0;
        SendPort = in.readInt();
        ReceivePort = in.readInt();
        Configured = true;
        in.readTypedList(Outlets, OutletInfo.CREATOR);
    }

    public void setConfigured(boolean b) {
        Configured = b;
    }

    public boolean isConfigured() {
        return Configured;
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
            } else if (name.equals("DefaultPorts")) {
                di.DefaultPorts = reader.nextBoolean();
            } else if (name.equals("SendPort")) {
                di.SendPort = reader.nextInt();
            } else if (name.equals("ReceivePort")) {
                di.ReceivePort = reader.nextInt();
            } else if (name.equals("Outlets")) {
                di.Outlets = new ArrayList<OutletInfo>();
                reader.beginArray();
                while (reader.hasNext()) {
                    di.Outlets.add(OutletInfo.fromJSON(reader, di));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        di.Configured = true;
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
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("SendPort").value(SendPort);
        writer.name("ReceivePort").value(ReceivePort);
        writer.name("Outlets").beginArray();
        for (OutletInfo oi : Outlets) {
            oi.toJSON(writer);
        }
        writer.endArray();
        writer.endObject();
    }
}
