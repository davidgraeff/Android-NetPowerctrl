package oly.netpowerctrl.transfer;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;

/**
 * Just send the application record via NFC to open up the play store on the other device
 * if the application is not installed so far.
 */
public class NFC {
    public static NdefMessage createNdefMessage() {
        if (Build.VERSION.SDK_INT < 14) {
//            NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
//                    "application/oly.netpowerctrl".getBytes(),
//                    new byte[0], new byte[0]);
            return new NdefMessage(new NdefRecord[]{
                    //   mimeRecord,
                    NdefRecord.createApplicationRecord("oly.netpowerctrl")
            });
        } else {
            return new NdefMessage(
                    //   NdefRecord.createMime("application/oly.netpowerctrl", new byte[0]),
                    NdefRecord.createApplicationRecord("oly.netpowerctrl")
            );
        }
    }

    // Check to see that the Activity started due to an Android Beam
    public static void checkIntentForNFC(Context context, Intent intent) {
        String intentAction = intent.getAction();

//        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intentAction)) {
//            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//            // only one message sent during the beam
//            assert rawMessages != null;
//            NdefMessage msg = (NdefMessage) rawMessages[0];
//            NFC.parseNFC(context, new String(msg.getRecords()[0].getPayload()));
//        }
    }
}
