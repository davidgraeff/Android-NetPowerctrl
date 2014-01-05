package oly.netpowerctrl.datastructure;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

//this class holds the info about a single outlet
public class OutletInfo implements Parcelable, Comparable {
    public int OutletNumber;
    private String Description;
    private String UserDescription;
    public boolean State;
    public boolean Disabled;
    public DeviceInfo device;
    public boolean Hidden;
    public int positionRequest;

    public String getDescription() {
        return (this.UserDescription.isEmpty() ? this.Description : this.UserDescription);
    }

    /**
     * Reset description set by the user, if the device propagates a new
     * description.
     *
     * @param desc The new description received from the device
     */
    public void setDescriptionByDevice(String desc) {
        if (!Description.equals(desc)) {
            if (!Description.isEmpty())
                UserDescription = "";
            Description = desc;
        }
    }

    public void setDescriptionByUser(String desc) {
        UserDescription = desc;
    }

    public OutletInfo(DeviceInfo di) {
        device = di;
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
        device = other.device;
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
            return getDescription().compareTo(other.getDescription());
        } else {
            return positionRequest < other.positionRequest ? -1 : 1;
        }
    }

    public static OutletInfo fromJSON(JsonReader reader, DeviceInfo di) throws IOException {
        reader.beginObject();
        OutletInfo oi = new OutletInfo(di);
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("OutletNumber")) {
                oi.OutletNumber = reader.nextInt();
            } else if (name.equals("Description")) {
                oi.Description = reader.nextString();
            } else if (name.equals("UserDescription")) {
                oi.UserDescription = reader.nextString();
            } else if (name.equals("State")) {
                oi.State = reader.nextBoolean();
            } else if (name.equals("Disabled")) {
                oi.Disabled = reader.nextBoolean();
            } else if (name.equals("Hidden")) {
                oi.Hidden = reader.nextBoolean();
            } else if (name.equals("positionRequest")) {
                oi.positionRequest = reader.nextInt();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        return oi;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("OutletNumber").value(OutletNumber);
        writer.name("Description").value(Description);
        writer.name("UserDescription").value(UserDescription);
        writer.name("State").value(State);
        writer.name("Disabled").value(Disabled);
        writer.name("Hidden").value(Hidden);
        writer.name("positionRequest").value(positionRequest);
        writer.endObject();
    }

    public String getDeviceDescription() {
        return Description;
    }
}

