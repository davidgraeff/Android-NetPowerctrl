package oly.netpowerctrl.datastructure;

import android.os.Parcel;
import android.os.Parcelable;

//this class holds the info about a single outlet
public class OutletInfo implements Parcelable {
    public int OutletNumber;
    public String Description;
    public String UserDescription;
    public boolean State;
    public boolean Disabled;
    public DeviceInfo device;
    public boolean Hidden;

    public OutletInfo() {
        OutletNumber = -1;
        Description = "";
        UserDescription = "";
        State = false;
        Disabled = false;
        Hidden = false;
    }

    public OutletInfo(OutletInfo other) {
        OutletNumber = other.OutletNumber;
        Description = other.Description;
        UserDescription = other.UserDescription;
        State = other.State;
        Disabled = other.Disabled;
        Hidden = other.Hidden;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(OutletNumber);
        dest.writeString(Description);
        dest.writeString(UserDescription);
        dest.writeInt(State ? 1 : 0);
        dest.writeInt(Disabled ? 1 : 0);
        dest.writeInt(Hidden ? 1 : 0);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<OutletInfo> CREATOR = new Parcelable.Creator<OutletInfo>() {
        public OutletInfo createFromParcel(Parcel in) {
            return new OutletInfo(in);
        }

        public OutletInfo[] newArray(int size) {
            return new OutletInfo[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private OutletInfo(Parcel in) {
        OutletNumber = in.readInt();
        Description = in.readString();
        UserDescription = in.readString();
        State = in.readInt() != 0;
        Disabled = in.readInt() != 0;
        Hidden = in.readInt() != 0;
    }
}

