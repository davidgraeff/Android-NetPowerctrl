package oly.netpowerctrl.datastructure;

import android.os.Parcel;
import android.os.Parcelable;

//this class holds the info about a single outlet
public class OutletInfo implements Parcelable, Comparable {
    public int OutletNumber;
    public String Description;
    public String UserDescription;
    public boolean State;
    public boolean Disabled;
    public DeviceInfo device;
    public boolean Hidden;
    public int positionRequest;

    public OutletInfo() {
        OutletNumber = -1;
        Description = "";
        UserDescription = "";
        State = false;
        Disabled = false;
        Hidden = false;
        positionRequest = 0;
    }

    public OutletInfo(OutletInfo other) {
        OutletNumber = other.OutletNumber;
        Description = other.Description;
        UserDescription = other.UserDescription;
        State = other.State;
        Disabled = other.Disabled;
        Hidden = other.Hidden;
        positionRequest = other.positionRequest;
    }

    public boolean equals(OutletInfo other) {
        return (OutletNumber == other.OutletNumber) && device.equals(device);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(OutletNumber);
        destination.writeString(Description);
        destination.writeString(UserDescription);
        destination.writeInt(State ? 1 : 0);
        destination.writeInt(Disabled ? 1 : 0);
        destination.writeInt(Hidden ? 1 : 0);
        destination.writeInt(positionRequest);
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
        positionRequest = in.readInt();
    }

    @Override
    public int compareTo(Object o) {
        OutletInfo other = (OutletInfo) o;
        if (other.equals(this))
            return 0;

        if (positionRequest == 0 && other.positionRequest == 0) {
            return ((UserDescription.isEmpty() ? Description : UserDescription).compareTo(
                    (other.UserDescription.isEmpty() ? other.Description : other.UserDescription)));
        } else {
            return positionRequest < other.positionRequest ? -1 : 1;
        }
    }
}

