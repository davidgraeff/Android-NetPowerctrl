package oly.netpowerctrl;

import android.os.Parcel;
import android.os.Parcelable;

//this class holds the info about a single outlet
public class OutletInfo implements Parcelable {
		public String Description;
		public boolean State;
		
		public OutletInfo() {
			Description = "";
			State = false;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(Description);
			dest.writeInt(State ? 1 : 0);
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
	    	Description = in.readString();
	    	State = in.readInt() != 0;
	    }
}

