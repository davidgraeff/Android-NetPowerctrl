package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

// this class holds all the info about one device
public class DeviceInfo implements Parcelable {

	public String DeviceName; // name of the device as reported by UDP or configured by the user
	public String HostName;   // the hostname or ip address used to reach the device

	public String UserName;
	public String Password;
	
	public int SendPort;
	public int RecvPort;
	
	public List<OutletInfo> Outlets;

    public DeviceInfo() {
    	DeviceName = "";
    	HostName = "";
    	UserName = "";
    	Password = "";
    	SendPort = -1;
    	RecvPort = -1;
    	Outlets = new ArrayList<OutletInfo>();
    }
    
    public DeviceInfo(Context cx) {
    	this();
    	DeviceName = (String) cx.getResources().getText(R.string.default_device_name);
    	SendPort = cx.getResources().getInteger(R.integer.default_send_port);
    	RecvPort = cx.getResources().getInteger(R.integer.default_recv_port);
    }

    public DeviceInfo(DeviceInfo other) {
    	DeviceName = other.DeviceName;
    	HostName = other.HostName;
    	UserName = other.UserName;
    	Password = other.Password;
    	SendPort = other.SendPort;
    	RecvPort = other.RecvPort;
    	Outlets = new ArrayList<OutletInfo>();
    	for (OutletInfo oi: other.Outlets)
    		Outlets.add(new OutletInfo(oi));
    }
    
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(DeviceName);
		dest.writeString(HostName);
		dest.writeString(UserName);
		dest.writeString(Password);
		dest.writeInt(SendPort);
		dest.writeInt(RecvPort);
		dest.writeList(Outlets);
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
		DeviceName = in.readString();
		HostName = in.readString();
		UserName = in.readString();
		Password = in.readString();
		SendPort = in.readInt();
		RecvPort = in.readInt();
		in.readTypedList(Outlets, OutletInfo.CREATOR);
    }
	
}
