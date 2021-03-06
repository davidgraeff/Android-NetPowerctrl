package oly.netpowerctrl.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import oly.netpowerctrl.main.MainActivity;

/**
 * Donate via inapp purchase
 */
public class Donate {
    private static final int INAPP_REQUEST = 12345;
    private WeakReference<Activity> activityWeakReference;
    private IInAppBillingService mService;
    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final Activity activity = activityWeakReference.get();
            if (activity == null)
                return;

            mService = IInAppBillingService.Stub.asInterface(service);
            activity.invalidateOptionsMenu();

            new Thread("donateThread") {

                @Override
                public void run() {
                    try {
                        Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), "inapp", null);

                        int response = ownedItems.getInt("RESPONSE_CODE");
                        if (response == 0) {
                            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

                            if (purchaseDataList != null) {
                                for (String purchaseData : purchaseDataList) {
                                    JSONObject json = new JSONObject(purchaseData);
                                    mService.consumePurchase(3, activity.getPackageName(), json.getString("purchaseToken"));
                                }
                            }
                        }
                    } catch (RemoteException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    public void onActivityResult(final Activity activity, int requestCode, int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == INAPP_REQUEST) {
            Toast.makeText(activity, "Thank you!", Toast.LENGTH_LONG).show();

            new Thread() {

                @Override
                public void run() {
                    try {
                        JSONObject json = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
                        mService.consumePurchase(3, activity.getPackageName(), json.getString("purchaseToken"));
                    } catch (JSONException | RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }.start();
        }
    }

    public void onDestroy(Activity activity) {
        activity.unbindService(mServiceConn);
    }

    public boolean donatePossible() {
        return mService != null;
    }

    public void buy(String sku) {
        Activity activity = activityWeakReference.get();
        if (activity == null)
            return;

        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, activity.getPackageName(), sku, "inapp", "bGoa+V7g/ysDXvKwqq+JTFn4uQZbPiQJo4pf9RzJ");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null) {
                activity.startIntentSenderForResult(pendingIntent.getIntentSender(), INAPP_REQUEST,
                        new Intent(), 0, 0, 0);
            }
        } catch (RemoteException | IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    public void start(MainActivity mainActivity) {
        Intent i = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        i.setPackage("com.android.vending");
        mainActivity.bindService(i, mServiceConn, Context.BIND_AUTO_CREATE);
        activityWeakReference = new WeakReference<Activity>(mainActivity);
    }
}
