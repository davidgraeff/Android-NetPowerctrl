package oly.netpowerctrl.datastructure;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.service.DeviceQuery;

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
        SendPort = DeviceQuery.getDefaultSendPort(cx);
        ReceivePort = DeviceQuery.getDefaultRecvPort(cx);
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

}
