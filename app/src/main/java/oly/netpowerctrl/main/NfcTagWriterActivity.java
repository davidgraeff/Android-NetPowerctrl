package oly.netpowerctrl.main;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.ndeftools.Message;
import org.ndeftools.MimeRecord;
import org.ndeftools.externaltype.AndroidApplicationRecord;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.SharedPrefs;


/**
 * Activity demonstrating the default implementation of the abstract tag writer activity.
 * <p/>
 * The activity uses a simple layout and displays some toast messages for various events.
 *
 * @author Thomas Rorvik Skjolberg
 */

public class NfcTagWriterActivity extends org.ndeftools.util.activity.NfcTagWriterActivity {
    private UUID uuid;
    private TextView textView;
    private String lastMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle extra = getIntent().getExtras();

        try {
            if (extra != null)
                uuid = UUID.fromString(extra.getString("uuid", ""));
            else
                uuid = null;
        } catch (IllegalArgumentException e) {
            uuid = null;
        }
        String name = extra != null ? extra.getString("name") : getString(R.string.app_name);

        super.onCreate(savedInstanceState);

        // set theme based on user preference
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        setContentView(R.layout.activity_nfc_writer);

        ((TextView) findViewById(R.id.title)).setText(getString(R.string.nfc_writer_title, name));
        textView = ((TextView) findViewById(R.id.state));
        if (lastMessage != null)
            message(lastMessage);

        setDetecting(true);
    }

    /**
     * Create an NDEF message to be written when a tag is within range.
     *
     * @return the message to be written
     */

    @Override
    protected NdefMessage createNdefMessage() {

        // compose our own message
        Message message = new Message();

        if (uuid != null) {
            // add a Text Record with the message which is entered
            MimeRecord textRecord = new MimeRecord();
            textRecord.setMimeType("application/oly.netpowerctrl");
            textRecord.setData(uuid.toString().getBytes());
            message.add(textRecord);
        }

        // add an Android Application Record so that this app is launches if a tag is scanned :-)
        AndroidApplicationRecord androidApplicationRecord = new AndroidApplicationRecord();
        androidApplicationRecord.setPackageName(getPlayIdentifier());
        message.add(androidApplicationRecord);

        return message.getNdefMessage();
    }

    /**
     * Get Google Play application identifier
     *
     * @return
     */

    private String getPlayIdentifier() {
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.applicationInfo.packageName;
        } catch (final NameNotFoundException e) {
            return getClass().getPackage().getName();
        }
    }

    /**
     * Writing NDEF message to tag failed.
     *
     * @param e exception
     */

    @Override
    protected void writeNdefFailed(Exception e) {
        message(getString(R.string.nfc_WriteFailed, e.getMessage()));
    }

    /**
     * Tag is not writable or write-protected.
     */

    @Override
    public void writeNdefNotWritable() {
        message(getString(R.string.nfc_tagNotWritable));
    }

    /**
     * Tag capacity is lower than NDEF message size.
     */

    @Override
    public void writeNdefTooSmall(int required, int capacity) {
        message(getString(R.string.nfc_tagTooSmallMessage, required, capacity));
    }


    /**
     * Unable to write this type of tag.
     */

    @Override
    public void writeNdefCannotWriteTech() {
        message(getString(R.string.nfc_cannotWriteTechMessage));
    }

    /**
     * Successfully wrote NDEF message to tag.
     */

    @Override
    protected void writeNdefSuccess() {
        Toast.makeText(this, R.string.nfc_WriteSuccess, Toast.LENGTH_SHORT).show();
        finish();
        Log.w("writeNdefSuccess", uuid.toString());
    }

    /**
     * NFC feature was found and is currently enabled
     */

    @Override
    protected void onNfcStateEnabled() {
        message(getString(R.string.nfc_AvailableEnabled));
    }

    /**
     * NFC feature was found but is currently disabled
     */

    @Override
    protected void onNfcStateDisabled() {
        message(getString(R.string.nfc_AvailableDisabled));
    }

    /**
     * NFC setting changed since last check. For example, the user enabled NFC in the wireless settings.
     */

    @Override
    protected void onNfcStateChange(boolean enabled) {
        if (enabled) {
            onNfcStateEnabled();
        } else {
            onNfcStateDisabled();
        }
    }

    /**
     * This device does not have NFC hardware
     */

    @Override
    protected void onNfcFeatureNotFound() {
        message(getString(R.string.nfc_noNfcMessage));
    }

    public void message(String message) {
        if (textView != null)
            textView.setText(message);
        else
            lastMessage = message;
    }
}